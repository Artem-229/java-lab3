import java.util.*;
import java.util.concurrent.*;

public class Dispatcher {
    private final List<Elevator> elevators;
    private final BlockingQueue<Request> externalRequests;
    private volatile boolean running;
    private ElevatorSystem system;
    
    public Dispatcher(List<Elevator> elevators) {
        this.elevators = new ArrayList<>(elevators);
        this.externalRequests = new LinkedBlockingQueue<>();
        this.running = true;
        if (!elevators.isEmpty()) {
            this.system = elevators.get(0).getSystem();
        }
    }
    
    public void start() {
        Thread dispatcherThread = new Thread(this::dispatchLoop, "Dispatcher");
        dispatcherThread.start();
    }
    
    // Основной цикл диспетчера для обработки запросов из очереди
    private void dispatchLoop() {
        log("Диспетчер запущен");
        
        while (running) {
            try {
                Request request = externalRequests.poll(200, TimeUnit.MILLISECONDS);
                if (request != null) {
                    assignRequest(request);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log("Диспетчер остановлен");
    }
    
    public void addExternalRequest(int floor, Direction direction) {
        if (!isValidFloor(floor)) {
            log("Ошибка: неверный этаж " + floor);
            return;
        }
        
        if (!isValidDirection(floor, direction)) {
            log("Ошибка: неверное направление для этажа " + floor + " - " + direction);
            return;
        }
        
        Request request = new Request(floor, -1, direction, RequestType.EXTERNAL);
        externalRequests.offer(request);
        log("Новый вызов: этаж " + floor + ", направление " + direction);
    }

    public void addInternalRequest(int targetFloor, int elevatorId) {
        if (!isValidFloor(targetFloor)) {
            log("Ошибка: неверный целевой этаж " + targetFloor);
            return;
        }
        
        if (elevatorId >= 0 && elevatorId < elevators.size()) {
            Request request = new Request(targetFloor, elevatorId);
            elevators.get(elevatorId).addRequest(request);
            log("Внутренний вызов на этаж " + targetFloor + " для лифта " + elevatorId);
        } else {
            log("Ошибка: неверный ID лифта " + elevatorId);
        }
    }

    private boolean isValidFloor(int floor) {
        return floor >= 1 && floor <= BuildingConfig.FLOORS;
    }

    private boolean isValidDirection(int floor, Direction direction) {
        if (floor == 1 && direction == Direction.DOWN) return false;
        if (floor == BuildingConfig.FLOORS && direction == Direction.UP) return false;
        return true;
    }
    
    private void assignRequest(Request request) {
        Elevator bestElevator = findBestElevator(request);
        if (bestElevator != null) {
            try {
                if (bestElevator.addRequestWithTimeout(request, 100, TimeUnit.MILLISECONDS)) {
                    log("Вызов с этажа " + request.getSourceFloor() + " назначен лифту " + bestElevator.getIdNum());
                } else {
                    log("Не удалось назначить вызов лифту " + bestElevator.getIdNum() + ", выбираем другой");
                    assignToNextBest(request, bestElevator);
                }
            } catch (Exception e) {
                log("Ошибка при назначении лифту " + bestElevator.getIdNum() + ": " + e.getMessage());
            }
        }
    }

    private void assignToNextBest(Request request, Elevator excluded) {
        Elevator best = null;
        int bestScore = Integer.MAX_VALUE;
        
        for (Elevator elevator : elevators) {
            if (elevator == excluded) continue;
            
            int score = calculateScore(elevator, request);
            if (score < bestScore) {
                bestScore = score;
                best = elevator;
            }
        }
        
        if (best != null) {
            best.addRequest(request);
            log("Вызов переназначен лифту " + best.getIdNum());
        }
    }
    
    private Elevator findBestElevator(Request request) {
        Elevator best = null;
        int bestScore = Integer.MAX_VALUE;
        
        for (Elevator elevator : elevators) {
            int score = calculateScore(elevator, request);
            if (score < bestScore) {
                bestScore = score;
                best = elevator;
            }
        }
        
        return best;
    }
    
    private int calculateScore(Elevator elevator, Request request) {
    int currentFloor = elevator.getCurrentFloor();
    int requestFloor = request.getSourceFloor();
    Direction requestDir = request.getDirection();
    Direction elevatorDir = elevator.getDirection();
    ElevatorState state = elevator.getElevatorState();
    
    if (state == ElevatorState.IDLE) {
        return Math.abs(currentFloor - requestFloor);
    }
    
    if (elevatorDir == requestDir) {
        if (elevatorDir == Direction.UP && requestFloor >= currentFloor) {
            int distance = requestFloor - currentFloor;
            int stopsPenalty = countStopsBetween(elevator, currentFloor, requestFloor);
            return distance + stopsPenalty * 2;
        }
        if (elevatorDir == Direction.DOWN && requestFloor <= currentFloor) {
            int distance = currentFloor - requestFloor;
            int stopsPenalty = countStopsBetween(elevator, requestFloor, currentFloor);
            return distance + stopsPenalty * 2;
        }
    }
    
    List<Integer> targets = elevator.getTargetFloors();
    int remainingStops = targets.size();
    int furthestTarget = getFurthestTarget(elevator, targets);
    int distanceToFinish = Math.abs(currentFloor - furthestTarget);
    int distanceAfterFinish = Math.abs(furthestTarget - requestFloor);
    
    return distanceToFinish + distanceAfterFinish + remainingStops * 10;
    }

    private int countStopsBetween(Elevator elevator, int from, int to) {
        int stops = 0;
        List<Integer> targets = elevator.getTargetFloors();
        for (int target : targets) {
            if (target >= from && target <= to) {
                stops++;
            }
        }
        return stops;
    }

    private int getFurthestTarget(Elevator elevator, List<Integer> targets) {
        if (targets.isEmpty()) return elevator.getCurrentFloor();
        
        int current = elevator.getCurrentFloor();
        Direction dir = elevator.getDirection();
        
        if (dir == Direction.UP) {
            return Collections.max(targets);
        } else if (dir == Direction.DOWN) {
            return Collections.min(targets);
        }
        return current;
    }
    
    private void log(String message) {
        System.out.println(message);
        if (system != null && system.getGUI() != null) {
            system.getGUI().addLog(message);
        }
    }
    
    public void stop() {
        running = false;
    }
}