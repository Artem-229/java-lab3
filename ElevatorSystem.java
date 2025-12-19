import java.util.*;
import javax.swing.*;

public class ElevatorSystem {
    private final List<Elevator> elevators;
    private final Dispatcher dispatcher;
    private ElevatorGUI gui;
    
    public ElevatorSystem(int elevatorsCount) {
        this.elevators = new ArrayList<>();
        
        for (int i = 0; i < elevatorsCount; i++) {
            Elevator elevator = new Elevator(i, i + 1, this);
            elevators.add(elevator);
        }
        
        this.dispatcher = new Dispatcher(elevators);
    }
    
    public void setGUI(ElevatorGUI gui) {
        this.gui = gui;
    }
    
    public ElevatorGUI getGUI() {
        return gui;
    }
    
    // Запуск диспетчера и всех лифтов
    public void start() {
        System.out.println("=== Запуск системы лифтов ===");
        
        dispatcher.start();
        
        for (Elevator elevator : elevators) {
            elevator.start();
        }
        
        if (gui != null) {
            gui.addLog("Система запущена с " + elevators.size() + " лифтами");
        }
        
        System.out.println("Система запущена с " + elevators.size() + " лифтами");
    }
    
    public void stop() {
        System.out.println("\n=== Остановка системы ===");
    
        dispatcher.stop();
        
        for (Elevator elevator : elevators) {
            elevator.interrupt();
        }
        
        for (Elevator elevator : elevators) {
            try {
                elevator.join(2000); 
                if (elevator.isAlive()) {
                    System.out.println("Лифт " + elevator.getIdNum() + " не остановился корректно");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (gui != null) {
            gui.addLog("Система остановлена");
            gui.stop();
        }
        
        System.out.println("Система остановлена");
    }
    
    public Dispatcher getDispatcher() {
        return dispatcher;
    }
    
    public List<Elevator> getElevators() {
        return elevators;
    }
    
    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0 && args[0].equals("nogui")) {
            // Консольный режим
            ElevatorSystem system = new ElevatorSystem(BuildingConfig.ELEVATORS_COUNT);
            system.start();
            
            Thread.sleep(2000);
            
            system.getDispatcher().addExternalRequest(5, Direction.UP);
            system.getDispatcher().addExternalRequest(8, Direction.DOWN);
            system.getDispatcher().addInternalRequest(10, 0);
            
            Thread.sleep(3000);
            system.getDispatcher().addExternalRequest(3, Direction.UP);
            system.getDispatcher().addInternalRequest(15, 1);
            
            Thread.sleep(30000);
            
            system.stop();
        } else {
            // GUI режим (по умолчанию)
            SwingUtilities.invokeLater(() -> {
                ElevatorSystem system = new ElevatorSystem(BuildingConfig.ELEVATORS_COUNT);
                ElevatorGUI gui = new ElevatorGUI(system);
                system.setGUI(gui);
                system.start();
                gui.setVisible(true);
            });
        }
    }
}