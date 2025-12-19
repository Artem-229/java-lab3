import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Elevator extends Thread {
    private final int id;
    private int currentFloor;
    private Direction direction;
    private ElevatorState status;
    private final TreeSet<Integer> targetFloors;
    private final ReentrantLock lock;
    private final BlockingQueue<Request> requestQueue;
    private final ElevatorSystem system;
    
    public Elevator(int id, int startFloor, ElevatorSystem system) {
        this.id = id;
        this.currentFloor = startFloor;
        this.direction = Direction.NONE;
        this.status = ElevatorState.IDLE;
        this.targetFloors = new TreeSet<>();
        this.lock = new ReentrantLock();
        this.requestQueue = new LinkedBlockingQueue<>();
        this.system = system;
        setName("Elevator-" + id);
    }
    
    public ElevatorSystem getSystem() {
        return system;
    }
    
    @Override
    public void run() {
        logToGUI(getName() + " запущен на этаже " + currentFloor);
        
        try {
            while (!Thread.currentThread().isInterrupted()) {
                processRequests();
                moveToTarget();
                checkArrival();
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Движение лифта к следующей цели с изменением этажа
    private void moveToTarget() {
        lock.lock();
        try {
            if (targetFloors.isEmpty()) {
                status = ElevatorState.IDLE;
                direction = Direction.NONE;
                return;
            }
            
            if (status == ElevatorState.IDLE || status == ElevatorState.MOVING) {
                int targetFloor = getNextTarget();
                
                if (targetFloor > currentFloor) {
                    direction = Direction.UP;
                    currentFloor++;
                    status = ElevatorState.MOVING;
                } else if (targetFloor < currentFloor) {
                    direction = Direction.DOWN;
                    currentFloor--;
                    status = ElevatorState.MOVING;
                }
                
                if (status == ElevatorState.MOVING) {
                    logToGUI(getName() + " едет " + direction + " на этаж " + currentFloor);
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    public List<Integer> getTargetFloors() {
        lock.lock();
        try {
            return new ArrayList<>(targetFloors);
        } finally {
            lock.unlock();
        }
    }

    // Проверка прибытия и обработка дверей
    private void checkArrival() {
        boolean arrived = false;
        lock.lock();
        try {
            if (targetFloors.contains(currentFloor)) {
                targetFloors.remove(currentFloor);
                arrived = true;
                status = ElevatorState.DOORS_OPENING; 
            }
        } finally {
            lock.unlock(); 
        }
        
        if (arrived) {
            logToGUI(getName() + " прибыл на этаж " + currentFloor + ". Открывает двери.");
            
            try {
                Thread.sleep(1000);  
                
                lock.lock();
                try {
                    status = ElevatorState.LOADING;
                } finally {
                    lock.unlock();
                }
                
                logToGUI(getName() + " посадка/высадка");
                Thread.sleep(1500);
                
                lock.lock();
                try {
                    status = ElevatorState.DOORS_CLOSING;
                } finally {
                    lock.unlock();
                }
                
                logToGUI(getName() + " закрывает двери");
                Thread.sleep(1000);
                
                lock.lock();
                try {
                    status = targetFloors.isEmpty() ? 
                            ElevatorState.IDLE : ElevatorState.MOVING;
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean addRequestWithTimeout(Request request, long timeout, TimeUnit unit) {
        try {
            return requestQueue.offer(request, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private int getNextTarget() {
        lock.lock();
        try {
            if (targetFloors.isEmpty()) return currentFloor;
            
            if (direction == Direction.UP) {
                for (int floor : targetFloors) {
                    if (floor >= currentFloor) {  
                        return floor;
                    }
                }
                return Collections.max(targetFloors);
            } else if (direction == Direction.DOWN) {
                for (int floor : targetFloors) {
                    if (floor <= currentFloor) { 
                        return floor;
                    }
                }
                return Collections.min(targetFloors);
            } else {
                int closest = currentFloor;
                int minDiff = Integer.MAX_VALUE;
                for (int floor : targetFloors) {
                    int diff = Math.abs(floor - currentFloor);
                    if (diff < minDiff) {
                        minDiff = diff;
                        closest = floor;
                    }
                }
                return closest;
            }
        } finally {
            lock.unlock();
        }
    }
    
    // Обработка входящих запросов из очереди лифта
    private void processRequests() throws InterruptedException {
        Request request = requestQueue.poll(100, TimeUnit.MILLISECONDS);
        if (request != null) {
            lock.lock();
            try {
                if (request.getType() == RequestType.EXTERNAL) {
                    targetFloors.add(request.getSourceFloor());
                    logToGUI(getName() + " получил вызов на этаж " + request.getSourceFloor());
                } else {
                    targetFloors.add(request.getTargetFloor());
                    logToGUI(getName() + " цель внутри: этаж " + request.getTargetFloor());
                }
            } finally {
                lock.unlock();
            }
        }
    }
    
    private void logToGUI(String message) {
        System.out.println(message);
        if (system != null && system.getGUI() != null) {
            system.getGUI().addLog(message);
        }
    }
    
    public void addRequest(Request request) {
        requestQueue.offer(request);
    }
    
    public int getCurrentFloor() {
        lock.lock();
        try {
            return currentFloor;
        } finally {
            lock.unlock();
        }
    }
    
    public Direction getDirection() {
        lock.lock();
        try {
            return direction;
        } finally {
            lock.unlock();
        }
    }
    
    public ElevatorState getElevatorState() {
        lock.lock();
        try {
            return status;
        } finally {
            lock.unlock();
        }
    }
    
    public String getTargetsString() {
        lock.lock();
        try {
            return targetFloors.toString();
        } finally {
            lock.unlock();
        }
    }
    
    public int getIdNum() {
        return id;
    }
}