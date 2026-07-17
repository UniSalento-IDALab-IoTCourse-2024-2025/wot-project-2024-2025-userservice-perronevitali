package it.unisalento.faro.service;

import it.unisalento.faro.configuration.rabbitmq.RabbitMQConstants;
import it.unisalento.faro.configuration.rabbitmq.RabbitMQManager;
import it.unisalento.faro.configuration.rabbitmq.RabbitMQMessageTypes;
import it.unisalento.faro.domain.User;
import it.unisalento.faro.domain.Worker;
import it.unisalento.faro.dto.messagesDTO.AreaUnauthorizedMessage;
import it.unisalento.faro.dto.messagesDTO.FaroMessage;
import it.unisalento.faro.dto.login_and_registration.WorkerRegistrationDTO;
import it.unisalento.faro.dto.main.WorkerDTO;
import it.unisalento.faro.dto.otherDTO.PositionUpdateDTO;
import it.unisalento.faro.exceptions.EmailAlreadyExistsException;
import it.unisalento.faro.repositories.UserRepository;
import org.bson.Document;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WorkerService {

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordEncoder passwordEncoder;

    @Inject
    RabbitMQManager rabbitMQManager;

    public List<WorkerDTO> getAllWorkers() {
        List<User> workers = userRepository.list("_t", "worker");
        List<WorkerDTO> result = new ArrayList<>();
        for (User user : workers) {
            result.add(toWorkerDTO(user));
        }
        return result;
    }

    public List<WorkerDTO> getAllWorkersByArea(String areaId) {
        List<User> workers = userRepository.list(
                new Document("_t", "worker").append("currentAreaId", areaId)
        );
        List<WorkerDTO> result = new ArrayList<>();
        for (User user : workers) {
            result.add(toWorkerDTO(user));
        }
        return result;
    }

    public WorkerDTO getWorkerById(String id) throws Exception {
        User user = userRepository.findById(id);
        if (user == null || !(user instanceof Worker)) {
            throw new Exception("Worker non trovato");
        }
        return toWorkerDTO(user);
    }

    public WorkerDTO getWorkerByEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !(user instanceof Worker)) {
            throw new Exception("Worker non trovato");
        }
        return toWorkerDTO(user);
    }

    public WorkerDTO register(WorkerRegistrationDTO registrationDTO) throws EmailAlreadyExistsException {
        Optional<User> existingUser = userRepository.findByEmail(registrationDTO.getEmail());
        if (existingUser.isPresent()) {
            throw new EmailAlreadyExistsException();
        }

        Worker worker = new Worker();
        worker.setNome(registrationDTO.getNome());
        worker.setCognome(registrationDTO.getCognome());
        worker.setEmail(registrationDTO.getEmail());
        worker.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        worker.setAuthorizedAreaIds(new ArrayList<>());

        userRepository.persist(worker);

        try {
            rabbitMQManager.declareUserQueue(worker.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return toWorkerDTO(worker);
    }

    public WorkerDTO updateWorkerById(String id, WorkerDTO workerDto) throws Exception {
        User user = userRepository.findById(id);
        if (user == null || !(user instanceof Worker)) {
            throw new Exception("Worker non trovato");
        }

        Worker worker = (Worker) user;
        worker.setNome(workerDto.getNome());
        worker.setCognome(workerDto.getCognome());

        userRepository.update(worker);
        return toWorkerDTO(worker);
    }

    public WorkerDTO assignAreas(String id, ArrayList<String> areaIds) throws Exception {
        User user = userRepository.findById(id);
        if (user == null || !(user instanceof Worker)) {
            throw new Exception("Worker non trovato");
        }

        Worker worker = (Worker) user;
        worker.setAuthorizedAreaIds(areaIds);

        userRepository.update(worker);
        return toWorkerDTO(worker);
    }

    /**
     * Aggiorna l'area corrente del worker. Se l'area non è tra quelle
     * autorizzate, pubblica un AREA_UNAUTHORIZED sul topic dell'area
     * (visibile in tempo reale a tutti i client connessi) e propaga
     * l'informazione a OperationalService tramite un unico POSITION_UPDATE
     * arricchito con il flag "unauthorized".
     */
    public WorkerDTO updateCurrentArea(String userId, String areaId, String previousAreaId) throws Exception {
        User user = userRepository.findById(userId);
        if (user == null || !(user instanceof Worker)) {
            throw new Exception("Worker non trovato");
        }

        Worker worker = (Worker) user;
        worker.setCurrentAreaId(areaId);
        userRepository.update(worker);

        boolean isUnauthorized = worker.getAuthorizedAreaIds() == null
                || !worker.getAuthorizedAreaIds().contains(areaId);

        if (isUnauthorized) {
            try {
                // tutti gli utenti connessi all'area vedono l'accesso non autorizzato
                rabbitMQManager.publish(
                        RabbitMQConstants.EXCHANGE_AREAS,
                        "area." + areaId,
                        RabbitMQMessageTypes.AREA_UNAUTHORIZED,
                        new FaroMessage(
                                RabbitMQMessageTypes.AREA_UNAUTHORIZED,
                                new AreaUnauthorizedMessage(areaId)
                        )
                );
                System.out.println("AREA_UNAUTHORIZED pubblicato per area: " + areaId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // un'unica pubblicazione inter-servizio, con il flag già incluso:
        // OperationalService aggiorna sia workerIdsInArea che unauthorizedWorkerIds
        try {
            PositionUpdateDTO positionUpdate = new PositionUpdateDTO(
                    userId, areaId, previousAreaId, isUnauthorized
            );
            rabbitMQManager.publish(
                    RabbitMQConstants.EXCHANGE_AREA_UPDATES,
                    RabbitMQConstants.ROUTING_KEY_POSITION,
                    RabbitMQMessageTypes.POSITION_UPDATE,
                    positionUpdate
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return toWorkerDTO(worker);
    }

    public WorkerDTO deleteWorkerById(String id) throws Exception {
        User user = userRepository.findById(id);
        if (user == null || !(user instanceof Worker)) {
            throw new Exception("Worker non trovato");
        }

        userRepository.deleteById(id);
        return toWorkerDTO(user);
    }

    public List<WorkerDTO> getAuthorizedWorkersByArea(String areaId) {
        List<User> all = userRepository.list("_t", "worker");
        List<WorkerDTO> result = new ArrayList<>();
        for (User user : all) {
            Worker worker = (Worker) user;
            if (worker.getAuthorizedAreaIds() != null
                    && worker.getAuthorizedAreaIds().contains(areaId)) {
                result.add(toWorkerDTO(worker));
            }
        }
        return result;
    }

    public List<String> getAuthorizedAreaIds(String workerId) throws Exception {
        User user = userRepository.findById(workerId);
        if (user == null || !(user instanceof Worker)) {
            throw new Exception("Worker non trovato");
        }
        Worker worker = (Worker) user;
        if (worker.getAuthorizedAreaIds() == null) {
            return new ArrayList<>();
        }
        return worker.getAuthorizedAreaIds();
    }

    /**
     * Aggiunge un'area alla lista delle aree autorizzate del worker.
     * Chiamato da RabbitMQManager quando arriva un AUTHORIZE_AREA
     * (pubblicato da OperationalService alla conferma di una task).
     * Idempotente, no-op silenzioso se il worker non esiste più.
     */
    public void addAuthorizedArea(String workerId, String areaId) {
        User user = userRepository.findById(workerId);
        if (!(user instanceof Worker worker)) return;
        if (worker.getAuthorizedAreaIds() == null) {
            worker.setAuthorizedAreaIds(new ArrayList<>());
        }
        if (!worker.getAuthorizedAreaIds().contains(areaId)) {
            worker.getAuthorizedAreaIds().add(areaId);
            userRepository.update(worker);
        }
    }

    /**
     * Rimuove un'area dalla lista delle aree autorizzate del worker.
     * Chiamato da RabbitMQManager quando arriva un REVOKE_AREA
     * (pubblicato da OperationalService quando il worker completa/rifiuta
     * la sua parte di una task e non ha altre task attive in quell'area).
     * Idempotente, no-op silenzioso se il worker non esiste più.
     */
    public void removeAuthorizedArea(String workerId, String areaId) {
        User user = userRepository.findById(workerId);
        if (!(user instanceof Worker worker)) return;
        if (worker.getAuthorizedAreaIds() != null && worker.getAuthorizedAreaIds().remove(areaId)) {
            userRepository.update(worker);
        }
    }

    public WorkerDTO toWorkerDTO(User user) {
        Worker worker = (Worker) user;
        WorkerDTO dto = new WorkerDTO();
        dto.setId(worker.getId());
        dto.setNome(worker.getNome());
        dto.setCognome(worker.getCognome());
        dto.setEmail(worker.getEmail());
        dto.setCurrentAreaId(worker.getCurrentAreaId());
        if (worker.getAuthorizedAreaIds() != null) {
            dto.setAuthorizedAreaIds(worker.getAuthorizedAreaIds());
        } else {
            dto.setAuthorizedAreaIds(new ArrayList<>());
        }
        return dto;
    }
}
