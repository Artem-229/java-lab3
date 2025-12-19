import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ElevatorGUI extends JFrame {
    private final ElevatorSystem system;
    private JTable elevatorTable;
    private DefaultTableModel tableModel;
    private JTextArea logArea;
    private ScheduledExecutorService scheduler;
    
    private JSpinner callFloorSpinner;
    private JComboBox<Direction> callDirectionCombo;
    private JSpinner internalFloorSpinner;
    private JComboBox<Integer> elevatorIdCombo;
    private JButton randomGenButton;
    
    private JLabel statsLabel;
    private AtomicInteger totalRequests = new AtomicInteger(0);
    
    private boolean randomGenerationActive = false;
    private Thread randomGeneratorThread;
    
    public ElevatorGUI(ElevatorSystem system) {
        this.system = system;
        
        setTitle("Лифты - " + BuildingConfig.FLOORS + " этажей");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                int confirm = JOptionPane.showConfirmDialog(
                    ElevatorGUI.this, 
                    "Остановить систему лифтов перед выходом?",
                    "Подтверждение выхода", 
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    system.stop();
                    System.exit(0);
                }
            }
        });
        setSize(1200, 800);
        setLayout(new BorderLayout());
        
        // Темная тема
        getContentPane().setBackground(new Color(45, 45, 48));
        
        initComponents();
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(700);
        
        JPanel leftPanel = createLeftPanel();
        splitPane.setLeftComponent(leftPanel);
        
        JPanel rightPanel = createRightPanel();
        splitPane.setRightComponent(rightPanel);
        
        add(splitPane, BorderLayout.CENTER);
        
        JPanel logPanel = createLogPanel();
        add(logPanel, BorderLayout.SOUTH);
        
        startAutoRefresh();
    }
    
    private void initComponents() {
        String[] columns = {"ID", "Этаж", "Направление", "Статус", "Цели"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        elevatorTable = new JTable(tableModel);
        elevatorTable.setBackground(new Color(63, 63, 70));
        elevatorTable.setForeground(Color.WHITE);
        elevatorTable.setGridColor(Color.GRAY);
        
        logArea = new JTextArea();
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(Color.WHITE);
        
        callDirectionCombo = new JComboBox<>(new Direction[]{Direction.UP, Direction.DOWN});
        
        Integer[] elevatorIds = new Integer[BuildingConfig.ELEVATORS_COUNT];
        for (int i = 0; i < elevatorIds.length; i++) {
            elevatorIds[i] = i;
        }
        elevatorIdCombo = new JComboBox<>(elevatorIds);
        
        callFloorSpinner = new JSpinner(new SpinnerNumberModel(1, 1, BuildingConfig.FLOORS, 1));
        internalFloorSpinner = new JSpinner(new SpinnerNumberModel(1, 1, BuildingConfig.FLOORS, 1));

        statsLabel = new JLabel("Запросов: 0");
        statsLabel.setForeground(Color.WHITE);
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 48));
        TitledBorder border = BorderFactory.createTitledBorder("Состояние лифтов");
        border.setTitleColor(Color.WHITE); 
        panel.setBorder(border);
        
        elevatorTable.setRowHeight(30);
        elevatorTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        elevatorTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        elevatorTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        elevatorTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        elevatorTable.getColumnModel().getColumn(4).setPreferredWidth(200);
        
        for (int i = 0; i < BuildingConfig.ELEVATORS_COUNT; i++) {
            tableModel.addRow(new Object[]{i, "1", "NONE", "IDLE", "[]"});
        }
        
        panel.add(new JScrollPane(elevatorTable), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 48));
        TitledBorder border = BorderFactory.createTitledBorder("Управление");
        border.setTitleColor(Color.WHITE); 
        panel.setBorder(border);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(63, 63, 70));
        tabbedPane.setForeground(Color.WHITE);
        
        tabbedPane.addTab("Вызов", createCallTab());
        tabbedPane.addTab("Внутри лифта", createInternalTab());
        tabbedPane.addTab("Авто", createAutoGenTab());
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCallTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(63, 63, 70));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel l1 = new JLabel("Этаж:");
        l1.setForeground(Color.WHITE);
        panel.add(l1, gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(callFloorSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel l2 = new JLabel("Направление:");
        l2.setForeground(Color.WHITE);
        panel.add(l2, gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(callDirectionCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton btn = new JButton("Вызвать лифт");
        btn.setBackground(new Color(0, 122, 204));
        btn.setForeground(Color.WHITE);
        btn.addActionListener(e -> {
            int floor = (int) callFloorSpinner.getValue();
            Direction dir = (Direction) callDirectionCombo.getSelectedItem();
            system.getDispatcher().addExternalRequest(floor, dir);
            totalRequests.incrementAndGet();
            updateStats();
            addLog("Вызов: этаж " + floor + ", " + dir);
        });
        panel.add(btn, gbc);
        
        return panel;
    }
    
    private JPanel createInternalTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(63, 63, 70));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel l1 = new JLabel("Лифт:");
        l1.setForeground(Color.WHITE);
        panel.add(l1, gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(elevatorIdCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel l2 = new JLabel("Этаж:");
        l2.setForeground(Color.WHITE);
        panel.add(l2, gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(internalFloorSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton btn = new JButton("Нажать кнопку в лифте");
        btn.setBackground(new Color(0, 122, 204));
        btn.setForeground(Color.WHITE);
        btn.addActionListener(e -> {
            int floor = (int) internalFloorSpinner.getValue();
            int id = (int) elevatorIdCombo.getSelectedItem();
            system.getDispatcher().addInternalRequest(floor, id);
            totalRequests.incrementAndGet();
            updateStats();
            addLog("В лифте " + id + " нажали этаж " + floor);
        });
        panel.add(btn, gbc);
        
        return panel;
    }
    
    private JPanel createAutoGenTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(63, 63, 70));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel title = new JLabel("Случайные вызовы");
        title.setForeground(Color.WHITE);
        panel.add(title, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        randomGenButton = new JButton("Включить");
        randomGenButton.setBackground(new Color(0, 122, 204));
        randomGenButton.setForeground(Color.WHITE);
        randomGenButton.addActionListener(e -> toggleRandomGeneration());
        panel.add(randomGenButton, gbc);
        
        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 48));
        TitledBorder border = BorderFactory.createTitledBorder("Логи");
        border.setTitleColor(Color.WHITE);
        panel.setBorder(border);
        
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setRows(6);
        
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private void toggleRandomGeneration() {
        if (randomGenerationActive) {
            randomGenerationActive = false;
            randomGenButton.setText("Включить");
            if (randomGeneratorThread != null) {
                randomGeneratorThread.interrupt();
            }
            addLog("Автогенерация выключена");
        } else {
            randomGenerationActive = true;
            randomGenButton.setText("Выключить");
            startRandomGeneration();
            addLog("Автогенерация включена");
        }
    }
    
    private void startRandomGeneration() {
        randomGeneratorThread = new Thread(() -> {
            Random random = new Random();
            try {
                while (randomGenerationActive && !Thread.currentThread().isInterrupted()) {
                    if (random.nextBoolean()) {
                        int floor = random.nextInt(BuildingConfig.FLOORS) + 1;
                        Direction dir = random.nextBoolean() ? Direction.UP : Direction.DOWN;
                        system.getDispatcher().addExternalRequest(floor, dir);
                        totalRequests.incrementAndGet();
                    } else {
                        int id = random.nextInt(BuildingConfig.ELEVATORS_COUNT);
                        int floor = random.nextInt(BuildingConfig.FLOORS) + 1;
                        system.getDispatcher().addInternalRequest(floor, id);
                        totalRequests.incrementAndGet();
                    }
                    
                    Thread.sleep(2000 + random.nextInt(4000));
                    updateStats();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        randomGeneratorThread.start();
    }
    
    private void updateStats() {
        SwingUtilities.invokeLater(() -> {
            statsLabel.setText("Запросов: " + totalRequests.get());
        });
    }
    
    private void startAutoRefresh() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateStatus, 0, 500, TimeUnit.MILLISECONDS);
    }
    
    private void updateStatus() {
        SwingUtilities.invokeLater(() -> {
            if (system == null) return;
            
            List<Elevator> elevators = system.getElevators();
            if (elevators == null) return;
            
            for (int i = 0; i < elevators.size(); i++) {
                Elevator elevator = elevators.get(i);
                tableModel.setValueAt(elevator.getIdNum(), i, 0);
                tableModel.setValueAt(elevator.getCurrentFloor(), i, 1);
                tableModel.setValueAt(elevator.getDirection(), i, 2);
                tableModel.setValueAt(elevator.getElevatorState(), i, 3);
                tableModel.setValueAt(elevator.getTargetsString(), i, 4);
            }
        });
    }
    
    public void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String time = String.format("[%tT]", new java.util.Date());
            logArea.append(time + " " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (randomGeneratorThread != null) {
            randomGeneratorThread.interrupt();
        }
    }
}