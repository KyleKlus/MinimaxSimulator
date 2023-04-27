package de.uni_hannover.sra.minimax_simulator.ui.gui;

import de.uni_hannover.sra.minimax_simulator.Main;
import de.uni_hannover.sra.minimax_simulator.model.configuration.event.MachineConfigEvent;
import de.uni_hannover.sra.minimax_simulator.model.configuration.event.MachineConfigListEvent;
import de.uni_hannover.sra.minimax_simulator.model.configuration.event.MachineConfigListener;
import de.uni_hannover.sra.minimax_simulator.model.configuration.register.RegisterExtension;
import de.uni_hannover.sra.minimax_simulator.model.machine.simulation.Simulation;
import de.uni_hannover.sra.minimax_simulator.model.machine.simulation.SimulationListener;
import de.uni_hannover.sra.minimax_simulator.model.machine.simulation.SimulationState;
import de.uni_hannover.sra.minimax_simulator.model.machine.simulation.Traceable;
import de.uni_hannover.sra.minimax_simulator.model.signal.SignalRow;
import de.uni_hannover.sra.minimax_simulator.model.signal.SignalTable;
import de.uni_hannover.sra.minimax_simulator.model.signal.SignalTableListener;
import de.uni_hannover.sra.minimax_simulator.model.signal.jump.Jump;
import de.uni_hannover.sra.minimax_simulator.resources.TextResource;
import de.uni_hannover.sra.minimax_simulator.ui.UIUtil;
import de.uni_hannover.sra.minimax_simulator.ui.gui.components.dialogs.ExceptionDialog;
import de.uni_hannover.sra.minimax_simulator.ui.gui.components.dialogs.RegisterUpdateDialog;
import de.uni_hannover.sra.minimax_simulator.ui.gui.components.tableview.CenteredCellPane;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * <b>FXController of the DebuggerView</b><br>
 * <br>
 * This controller handles every GUI interaction with the debugger {@link Tab}.
 * The DebuggerView is the part of the UI for simulating the machine.
 *
 * @author Philipp Rohde
 */
public class DebuggerView implements SimulationListener, MachineConfigListener, SignalTableListener {

    private final TextResource resSignal;
    private final TextResource res;

    @FXML private TableView<RegisterTableModel> regTable;
    @FXML private TableColumn<RegisterTableModel, String> colRegName;
    @FXML private TableColumn<RegisterTableModel, String> colRegDec;
    @FXML private TableColumn<RegisterTableModel, String> colRegBin;
    @FXML private TableColumn<RegisterTableModel, String> colRegHex;

    @FXML private TableView<AluTableModel> aluTable;
    @FXML private TableColumn<AluTableModel, String> colAluDec;
    @FXML private TableColumn<AluTableModel, String> colAluBin;
    @FXML private TableColumn<AluTableModel, String> colAluHex;

    @FXML private TableView<SimulationTableModel> simTable;
    @FXML private TableColumn<SimulationTableModel, Boolean> colSim1;
    @FXML private TableColumn<SimulationTableModel, Boolean> colSim2;
    @FXML private TableColumn<SimulationTableModel, String> colSimLabel;
    @FXML private TableColumn<SimulationTableModel, String> colSimAdr;
    @FXML private TableColumn<SimulationTableModel, String> colSimAlu;
    @FXML private TableColumn<SimulationTableModel, String> colSimNext;
    @FXML private TableColumn<SimulationTableModel, String> colSimDesc;

    @FXML private TitledPane paneRegister;
    @FXML private TitledPane paneALU;
    @FXML private TitledPane paneSimulation;
    @FXML private Label lblCycles;

    @FXML private Button btnSimInit;
    @FXML private Button btnSimQuit;
    @FXML private Button btnSimCycle;
    @FXML private Button btnSimRun;

    private Simulation simulation;

    private final Tooltip simInit;
    private final Tooltip simStop;

    @FXML private MemoryTable embeddedMemoryTableController;

    private static final int NO_ROW_MARKED = -1;
    private static int lastExecutedRow = -1;

    private final MessageFormat cyclesFormatHalted;
    private final MessageFormat cyclesFormatRead;
    private final MessageFormat cyclesFormatWrite;
    private Object[] cyclesFormatParam = new Object[1];

    private static final Image INIT_SIM = new Image("/images/fugue/control-green.png");
    private static final Image RESET_SIM = new Image("/images/fugue/arrow-circle-225-red.png");

    /**
     * Initializes the final variables.
     */
    public DebuggerView() {
        resSignal = Main.getTextResource("signal");
        res = Main.getTextResource("debugger");

        simInit = new Tooltip(res.get("action.init.tip"));
        simStop = new Tooltip(res.get("action.reset.tip"));

        cyclesFormatHalted = res.createFormat("cycles.label");
        cyclesFormatRead = res.createFormat("cycles.read.label");
        cyclesFormatWrite = res.createFormat("cycles.write.label");
    }

    /**
     * This method is called during application start up and initializes the {code DebuggerView}
     * as much as possible without having any project data.
     */
    public void initialize() {
        setLocalizedTexts();
        setTooltips();
    }

    /**
     * Sets localized texts from resource for the GUI elements.
     */
    private void setLocalizedTexts() {
        final List<TableColumn> tableColumnsSignal = new ArrayList<>(Arrays.asList(colSimLabel, colSimAdr, colSimAlu, colSimNext, colSimDesc));
        for (TableColumn col : tableColumnsSignal) {
            col.setText(resSignal.get(col.getId().replace("_", ".")));
        }

        final List<TableColumn> tableColumns = new ArrayList<>(Arrays.asList(colRegName, colRegDec, colRegBin, colRegHex, colAluDec, colAluBin, colAluHex));
        for (TableColumn col : tableColumns) {
            col.setText(res.get(col.getId().replace("_", ".")));
        }

        final List<Labeled> controls = new ArrayList<>(Arrays.asList(paneRegister, paneALU, paneSimulation));
        for (Labeled con : controls) {
            con.setText(res.get(con.getId().replace("_", ".")));
        }

        lblCycles.setText(res.format(lblCycles.getId().replace("_", "."), "---"));
    }

    /**
     * Sets the {@link Tooltip}s for the {@link Button}s.
     */
    private void setTooltips() {
        btnSimCycle.setTooltip(new Tooltip(res.get("action.step.tip")));
        btnSimRun.setTooltip(new Tooltip(res.get("action.run.tip")));
        btnSimQuit.setTooltip(new Tooltip(res.get("action.stop.tip")));
        btnSimInit.setTooltip(simInit);
    }

    /**
     * This method is called from the main controller if a new project was created or opened.
     * It initializes the {@link TableView}s because they need project data.
     */
    public void initDebuggerView() {
        simulation = Main.getWorkspace().getProject().getSimulation();
        simulation.addSimulationListener(this);

        Main.getWorkspace().getProject().getMachineConfiguration().addMachineConfigListener(this);
        Main.getWorkspace().getProject().getSignalTable().addSignalTableListener(this);

        embeddedMemoryTableController.initMemTable();
        initRegTable();
        initAluTable();
        initSimulationTable();

        initSimulation();
        quitSimulation();
    }

    /**
     * Calls the update method for each {@link TableView}.
     */
    private void updateAllTables() {
        updateAluTable();
        updateRegTable();
        updateSimulationTable();
    }

    /**
     * Initializes the {@link TableView} for the registers.
     */
    private void initRegTable() {
        colRegName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRegDec.setCellValueFactory(new PropertyValueFactory<>("decimal"));
        colRegBin.setCellValueFactory(new PropertyValueFactory<>("binary"));
        colRegHex.setCellValueFactory(new PropertyValueFactory<>("hex"));

        // open edit dialog at double click
        regTable.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY) && mouseEvent.getClickCount() == 2
                    && simulation.getState() == SimulationState.IDLE) {
                String register = regTable.getSelectionModel().getSelectedItem().getName();
                Traceable<Integer> value = simulation.getRegisterValue(register);
                // open edit dialog
                Optional<ButtonType> result = new RegisterUpdateDialog(register, value).showAndWait();
                if (result.get() == ButtonType.OK) {
                    updateRegTable();
                }
            }
        });

        updateRegTable();
    }

    /**
     * Initializes the {@link TableView} for the ALU result.
     */
    private void initAluTable() {
        colAluDec.setCellValueFactory(new PropertyValueFactory<>("decimal"));
        colAluBin.setCellValueFactory(new PropertyValueFactory<>("binary"));
        colAluHex.setCellValueFactory(new PropertyValueFactory<>("hex"));

        updateAluTable();
    }

    /**
     * Updates the {@link TableView} for the registers.
     */
    private void updateRegTable() {
        ObservableList<RegisterTableModel> data = FXCollections.observableArrayList();
        List<RegisterExtension> registers = new ArrayList<>();
        registers.addAll(Main.getWorkspace().getProject().getMachineConfiguration().getBaseRegisters());
        registers.addAll(Main.getWorkspace().getProject().getMachineConfiguration().getRegisterExtensions());

        for (RegisterExtension register : registers) {
            String name = register.getName();
            Traceable<Integer> value = null;

            if (simulation.getState() != SimulationState.OFF) {
                value = simulation.getRegisterValue(name);
            }

            data.add(new RegisterTableModel(register, value));
        }

        regTable.setItems(data);
    }

    /**
     * Initializes the {@link TableView} for the simulation overview.
     */
    private void initSimulationTable() {
        colSim1.setCellValueFactory(new PropertyValueFactory<>("active"));
        colSim2.setCellValueFactory(new PropertyValueFactory<>("breakpoint"));
        colSimLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colSimAdr.setCellValueFactory(new PropertyValueFactory<>("address"));
        colSimAlu.setCellValueFactory(new PropertyValueFactory<>("alu"));
        colSimNext.setCellValueFactory(new PropertyValueFactory<>("next"));
        colSimDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        colSim2.setCellFactory(param -> {
            TableCell<SimulationTableModel, Boolean> cell = new TableCell<SimulationTableModel, Boolean>() {
                ImageView imageview = new ImageView();

                @Override
                public void updateItem(Boolean item, boolean empty) {
                    if (item != null && item) {
                        imageview.setImage(new Image("/images/fugue/control-record.png"));
                        setGraphic(new CenteredCellPane(imageview));
                    }
                    else {
                        setGraphic(null);
                    }
                }
            };

            cell.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                if (event.getClickCount() == 2 ) {
                    int index = cell.getTableView().getSelectionModel().getSelectedIndex();
                    SignalTable signalTable = Main.getWorkspace().getProject().getSignalTable();
                    SignalRow signalRow = signalTable.getRow(index);
                    signalRow.setBreakpoint(!signalRow.isBreakpoint());
                    updateSimulationTable();
                }
            });

            return cell;
        });

        colSim1.setCellFactory(param ->
            new TableCell<SimulationTableModel, Boolean>() {
                ImageView imageview = new ImageView();

                @Override
                public void updateItem(Boolean item, boolean empty) {
                    if (item != null && item) {
                        imageview.setImage(new Image("/images/fugue/arrow-curve-000-left.png"));
                        setGraphic(new CenteredCellPane(imageview));
                    } else {
                        setGraphic(null);
                    }
                }
            }
        );

        updateSimulationTable();
    }



    /**
     * Gets the index of the last executed row of the simulation.
     *
     * @return
     *          the index of the last executed row
     */
    public static int getLastExecutedRow() {
        return lastExecutedRow;
    }

    /**
     * Updates the {@link TableView} for the simulation overview.
     */
    private void updateSimulationTable() {
        ObservableList<SimulationTableModel> data = FXCollections.observableArrayList();

        SignalTable signalTable = Main.getWorkspace().getProject().getSignalTable();
        for (int i = 0; i < signalTable.getRowCount(); i++) {
            data.add(new SimulationTableModel(signalTable.getRow(i), i));
        }

        simTable.setItems(data);
    }

    /**
     * Updates the {@link TableView} for the ALU result.
     */
    private void updateAluTable() {
        ObservableList<AluTableModel> data = FXCollections.observableArrayList();

        if (simulation.getState().equals(SimulationState.OFF)) {
           data.add(new AluTableModel(null));
        }
        else {
            data.add(new AluTableModel(simulation.getAluResult().get()));
        }

        aluTable.setItems(data);
    }

    @Override
    public void stateChanged(SimulationState state) {
        if (state == SimulationState.IDLE) {
            // simulation has done a step
            lastExecutedRow = simulation.getCurrentSignalRow();
        }
        else if (state == SimulationState.HALTED) {
            // simulation ended
            if (lastExecutedRow != NO_ROW_MARKED) {
                lastExecutedRow = NO_ROW_MARKED;
            }
            btnSimCycle.setDisable(true);
            btnSimRun.setDisable(true);
        }
        else if (state == SimulationState.OFF) {
            // simulation canceled
            if (lastExecutedRow != NO_ROW_MARKED) {
                lastExecutedRow = NO_ROW_MARKED;
            }
            btnSimQuit.setDisable(true);
            btnSimCycle.setDisable(true);
            btnSimRun.setDisable(true);
            btnSimInit.setGraphic(new ImageView(INIT_SIM));
            btnSimInit.setTooltip(simInit);
            updateAllTables();
            updateCyclesText();
        }
    }

    /**
     * Initializes the simulation of the machine.
     */
    public void initSimulation() {
        try {
            if (simulation.getState().equals(SimulationState.OFF)) {
                simulation.init();
                btnSimQuit.setDisable(false);
                btnSimCycle.setDisable(false);
                btnSimRun.setDisable(false);
                btnSimInit.setGraphic(new ImageView(RESET_SIM));
                btnSimInit.setTooltip(simStop);
            }
            else {
                simulation.reset();
                btnSimCycle.setDisable(false);
                btnSimRun.setDisable(false);
            }
        } catch (Exception e) {
            UIUtil.invokeInFAT(() -> new ExceptionDialog(e).show());
        }

        updateAllTables();
        updateCyclesText();
    }

    /**
     * Stops the simulation of the machine and resets it.
     */
    public void quitSimulation() {
        try {
            simulation.stop();
            btnSimQuit.setDisable(true);
            btnSimCycle.setDisable(true);
            btnSimRun.setDisable(true);
            btnSimInit.setGraphic(new ImageView(INIT_SIM));
            btnSimInit.setTooltip(simInit);
        } catch (Exception e) {
            UIUtil.invokeInFAT(() -> new ExceptionDialog(e).show());
        }

        updateAllTables();
        updateCyclesText();
    }

    /**
     * Simulates the next cycle of the machine.
     */
    public void nextCycle() {
        try {
            simulation.step();
        } catch (Exception e) {
            UIUtil.invokeInFAT(() -> new ExceptionDialog(e).show());
        }

        updateAllTables();
        updateCyclesText();
    }

    /**
     * Simulates the machine until it reaches a breakpoint or the end of the program.
     */
    public void runSimulation() {
        UIUtil.executeWorker(() -> {
            // background task
            try {
                simulation.run();
            } catch (Exception e) {
                UIUtil.invokeInFAT(() -> new ExceptionDialog(e).show());
            }
        }, res.get("simulation.wait.title"), res.get("simulation.wait.message"), () -> simulation.pause());

        updateAllTables();
        updateCyclesText();
    }

    /**
     * Updates the cycle {@link Label} with the current cycle.
     */
    private void updateCyclesText() {
        String text;
        if (simulation.getState() == SimulationState.OFF) {
            cyclesFormatParam[0] = "---";
            text = cyclesFormatHalted.format(cyclesFormatParam);
        }
        else {
            MessageFormat format = simulation.isResolved() ? cyclesFormatWrite : cyclesFormatRead;
            cyclesFormatParam[0] = simulation.getCyclesCount();
            text = format.format(cyclesFormatParam);
        }
        lblCycles.setText(text);
    }

    @Override
    public void processEvent(MachineConfigEvent event) {
        if (event instanceof MachineConfigListEvent.MachineConfigRegisterEvent) {
            updateRegTable();
        }
    }

    @Override
    public void onStructureChanged() {
        updateSimulationTable();
    }

    @Override
    public void onRowAdded(int index, SignalRow row) {
        updateSimulationTable();
    }

    @Override
    public void onRowRemoved(int index) {
        updateSimulationTable();
    }

    @Override
    public void onRowsExchanged(int index1, int index2) {
        updateSimulationTable();
    }

    @Override
    public void onRowReplaced(int index, SignalRow row) {
        updateSimulationTable();
    }

    @Override
    public void onRowsUpdated(int fromIndex, int toIndex) {
        updateSimulationTable();
    }

    /**
     * This class represents the table model for the register {@link TableView}.<br>
     * <br>
     * It stores the name as well as the decimal and hexadecimal value as {@link SimpleStringProperty}.
     * The {@link RegisterExtension} is stored as well.
     *
     * @author Philipp Rohde
     */
    public static class RegisterTableModel {

        private final SimpleStringProperty name;
        private final SimpleStringProperty decimal;
        private final SimpleStringProperty binary;
        private final SimpleStringProperty hex;

        /**
         * Constructs a new {@code RegisterTableModel} and sets the properties.
         *
         * @param register
         *          the {@link RegisterExtension} to represent
         * @param value
         *          the traceable value of the register
         */
        private RegisterTableModel(RegisterExtension register, Traceable<Integer> value) {
            this.name = new SimpleStringProperty(register.getName());

            String decimal;
            String binary;
            String hex;
            if (value == null) {
                decimal = "--";
                binary = "--";
                hex = "--";
            }
            else {
                String formatString = register.getSize().getHexFormat();
                decimal = Integer.toString(value.get());
                binary = Integer.toString(value.get(), 2);
                hex = String.format(formatString, value.get());
            }
            this.decimal = new SimpleStringProperty(decimal);
            this.binary = new SimpleStringProperty(binary);
            this.hex = new SimpleStringProperty(hex);
        }

        /**
         * Gets the name of the register.
         *
         * @return
         *          the name of the register
         */
        public String getName() {
            return name.get();
        }

        /**
         * Sets the name of the register.
         *
         * @param name
         *          the new name of the register
         */
        public void setName(String name) {
            this.name.set(name);
        }

        /**
         * Gets the register value as decimal number.
         *
         * @return
         *          the register's value as decimal number
         */
        public String getDecimal() {
            return decimal.get();
        }

        /**
         * Sets the decimal representation of the register's value to the specified value.
         *
         * @param decimal
         *          the new decimal value
         */
        public void setDecimal(String decimal) {
            this.decimal.set(decimal);
        }

        /**
         * Gets the register value as binary number.
         *
         * @return
         *          the register's value as binary number
         */
        public String getBinary() {
            return binary.get();
        }

        /**
         * Sets the binary representation of the register's value to the specified value.
         *
         * @param binary
         *          the new decimal value
         */
        public void setBinary(String binary) {
            this.binary.set(binary);
        }

        /**
         * Gets the register value as hexadecimal number.
         *
         * @return
         *         the register's value as hexadecimal number
         */
        public String getHex() {
            return hex.get();
        }

        /**
         * Sets the hexadecimal representation of the register's value to the specified value.
         *
         * @param hex
         *          the new hexadecimal value
         */
        public void setHex(String hex) {
            this.hex.set(hex);
        }

    }

    /**
     * This class represents the table model for the ALU result {@link TableView}.<br>
     * <br>
     * It stores the decimal and hexadecimal value as {@link SimpleStringProperty}.
     *
     * @author Philipp Rohde
     */
    public static class AluTableModel {

        private final SimpleStringProperty decimal;
        private final SimpleStringProperty binary;
        private final SimpleStringProperty hex;

        /**
         * Constructs a new {@code AluTableModel} and sets the properties.
         *
         * @param value
         *          the ALU result
         */
        private AluTableModel (Integer value) {
            if (value == null) {
                this.decimal = new SimpleStringProperty("--");
                this.binary = new SimpleStringProperty("--");
                this.hex = new SimpleStringProperty("--");
            }
            else {
                this.decimal = new SimpleStringProperty(String.valueOf(value));
                this.binary = new SimpleStringProperty(Integer.toString(value, 2));
                this.hex = new SimpleStringProperty(String.format("0x%08X", value));
            }
        }

        /**
         * Gets the ALU result as decimal number.
         *
         * @return
         *         the ALU result as decimal number
         */
        public String getDecimal() {
            return decimal.get();
        }

        /**
         * Sets the decimal representation of the ALU result to the specified value.
         *
         * @param decimal
         *         the new decimal value
         */
        public void setDecimal(String decimal) {
            this.decimal.set(decimal);
        }

        /**
         * Gets the register value as binary number.
         *
         * @return
         *          the register's value as binary number
         */
        public String getBinary() {
            return binary.get();
        }

        /**
         * Sets the binary representation of the register's value to the specified value.
         *
         * @param binary
         *          the new decimal value
         */
        public void setBinary(String binary) {
            this.binary.set(binary);
        }

        /**
         * Gets the ALU result as hexadecimal number.
         *
         * @return
         *         the ALU result as hexadecimal number
         */
        public String getHex() {
            return hex.get();
        }

        /**
         * Sets the hexadecimal representation of the ALU result to the specified value.
         *
         * @param hex
         *         the new hexadecimal value
         */
        public void setHex(String hex) {
            this.hex.set(hex);
        }
    }

    /**
     * This class represents the table model for the simulation {@link TableView}.<br>
     * <br>
     * It stores the label as well as the address, ALU condition, jump target and description as {@link SimpleStringProperty}.
     * If the row is active and/or a breakpoint is stored as {@link SimpleBooleanProperty}.
     *
     * @author Philipp Rohde
     */
    public static class SimulationTableModel {

        private final SimpleStringProperty label;
        private final SimpleStringProperty address;
        private final SimpleStringProperty alu;
        private final SimpleStringProperty next;
        private final SimpleStringProperty description;
        private final SimpleBooleanProperty breakpoint;
        private final SimpleBooleanProperty active;

        /**
         * Constructs a new {@code SimulationTableModel} and sets the properties.
         *
         * @param row
         *          the {@link SignalRow} to represent
         * @param rowIndex
         *          the index of the row
         */
        private SimulationTableModel(SignalRow row, int rowIndex) {
            if (row.isBreakpoint()) {
                breakpoint = new SimpleBooleanProperty(true);
            }
            else {
                breakpoint = new SimpleBooleanProperty(false);
            }

            if (rowIndex == DebuggerView.getLastExecutedRow()) {
                active = new SimpleBooleanProperty(true);
            }
            else {
                active = new SimpleBooleanProperty(false);
            }

            this.label = new SimpleStringProperty(row.getLabel());
            this.address = new SimpleStringProperty(String.valueOf(rowIndex));

            Jump j = row.getJump();
            if (j.getTargetRow(rowIndex, 0) == j.getTargetRow(rowIndex, 1)) {
                this.alu = new SimpleStringProperty("-");
            }
            else {
                this.alu = new SimpleStringProperty("1\n0");
            }

            int target0 = j.getTargetRow(rowIndex, 0);
            int target1 = j.getTargetRow(rowIndex, 1);
            if (target0 == target1) {
                this.next = new SimpleStringProperty(Integer.toString(target0));
            }
            else {
                this.next = new SimpleStringProperty(target1 + "\n" + target0);
            }

            this.description = new SimpleStringProperty(row.getDescription());
        }

        /**
         * Gets the label of the {@code SignalRow}.
         *
         * @return
         *          the row's label
         */
        public String getLabel() {
            return label.get();
        }

        /**
         * Sets the row's label to the specified value.
         *
         * @param label
         *          the new label
         */
        public void setLabel(String label) {
            this.label.set(label);
        }

        /**
         * Gets the index of the {@code SignalRow}.
         *
         * @return
         *          the row's index
         */
        public String getAddress() {
            return address.get();
        }

        /**
         * Sets the index of the row.
         *
         * @param address
         *          the new index
         */
        public void setAddress(String address) {
            this.address.set(address);
        }

        /**
         * Gets the string representation of the ALU.ResultFlag check.
         *
         * @return
         *          the string representation of the flag
         */
        public String getAlu() {
            return alu.get();
        }

        /**
         * Sets the string representation of the ALU.ResultFlag to the specified value.
         *
         * @param alu
         *          the new value
         */
        public void setAlu(String alu) {
            this.alu.set(alu);
        }

        /**
         * Gets the string representation of the next row to be executed.
         *
         * @return
         *         the next row to be executed
         */
        public String getNext() {
            return next.get();
        }

        /**
         * Sets the string representation of the next row to be executed to the specified value.
         *
         * @param next
         *          the new value
         */
        public void setNext(String next) {
            this.next.set(next);
        }

        /**
         * Gets the description of the {@code SignalRow}.
         *
         * @return
         *         the row's description
         */
        public String getDescription() {
            return description.get();
        }

        /**
         * Sets the row's description to the specified value.
         *
         * @param description
         *          the new value
         */
        public void setDescription(String description) {
            this.description.set(description);
        }

        /**
         * Gets the value of the {@code isBreakpoint} property of the {@code SignalRow}.
         *
         * @return
         *          {@code true} if the row has a breakpoint, {@code false} otherwise
         */
        public Boolean getBreakpoint() {
            return breakpoint.get();
        }

        /**
         * Sets the {@code isBreakpoint} property to the specified value.
         *
         * @param breakpoint
         *          the new value
         */
        public void setBreakpoint(Boolean breakpoint) {
            this.breakpoint.set(breakpoint);
        }

        /**
         * Gets the value of the {@code isActive} property of the {@code SignalRow}.
         *
         * @return
         *          {@code true} if the row is currently executed, {@code false} otherwise
         */
        public Boolean getActive() {
            return active.get();
        }

        /**
         * Sets the {@code isActive} property to the specified value.
         *
         * @param active
         *         the new value
         */
        public void setActive(Boolean active) {
            this.active.set(active);
        }
    }

}
