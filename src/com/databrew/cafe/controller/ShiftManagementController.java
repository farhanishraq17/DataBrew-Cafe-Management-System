package com.databrew.cafe.controller;

import com.databrew.cafe.dao.AttendanceDao;
import com.databrew.cafe.dao.EmployeeDao;
import com.databrew.cafe.dao.ShiftDao;
import com.databrew.cafe.model.Attendance;
import com.databrew.cafe.model.Employee;
import com.databrew.cafe.model.Shift;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftManagementController {

    // ---- Shift table ----
    @FXML
    private TableView<Shift> shiftTable;
    @FXML
    private TableColumn<Shift, Number> shColId;
    @FXML
    private TableColumn<Shift, String> shColName;
    @FXML
    private TableColumn<Shift, String> shColStart;
    @FXML
    private TableColumn<Shift, String> shColEnd;
    @FXML
    private TextField shiftNameField;
    @FXML
    private TextField shiftStartField;
    @FXML
    private TextField shiftEndField;

    // ---- Attendance ----
    @FXML
    private ComboBox<String> empCombo;
    @FXML
    private ComboBox<String> attShiftCombo;
    @FXML
    private DatePicker attDatePicker;
    @FXML
    private DatePicker attFromDate;
    @FXML
    private DatePicker attToDate;

    @FXML
    private TableView<Attendance> attTable;
    @FXML
    private TableColumn<Attendance, Number> attColId;
    @FXML
    private TableColumn<Attendance, String> attColEmployee;
    @FXML
    private TableColumn<Attendance, String> attColShift;
    @FXML
    private TableColumn<Attendance, String> attColDate;
    @FXML
    private TableColumn<Attendance, String> attColCheckIn;
    @FXML
    private TableColumn<Attendance, String> attColCheckOut;
    @FXML
    private TableColumn<Attendance, String> attColStatus;

    private final ShiftDao shiftDao = new ShiftDao();
    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final EmployeeDao employeeDao = new EmployeeDao();

    private final ObservableList<Shift> shiftList = FXCollections.observableArrayList();
    private final ObservableList<Attendance> attList = FXCollections.observableArrayList();
    private Shift selectedShift;

    // lookup maps
    private final Map<Long, String> empNames = new HashMap<>();
    private final Map<Long, String> shiftNames = new HashMap<>();
    private final Map<String, Long> empNameToId = new HashMap<>();
    private final Map<String, Long> shiftNameToId = new HashMap<>();

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        // Shift columns
        shColId.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().getId()));
        shColName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));
        shColStart.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStartTime()));
        shColEnd.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEndTime()));
        shiftTable.setItems(shiftList);
        shiftTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> onShiftSelected(n));

        // Attendance columns
        attColId.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().getId()));
        attColEmployee.setCellValueFactory(cd -> new SimpleStringProperty(
                empNames.getOrDefault(cd.getValue().getEmployeeId(), String.valueOf(cd.getValue().getEmployeeId()))));
        attColShift.setCellValueFactory(cd -> new SimpleStringProperty(
                shiftNames.getOrDefault(cd.getValue().getShiftId(), String.valueOf(cd.getValue().getShiftId()))));
        attColDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getWorkDate() != null ? cd.getValue().getWorkDate().toString() : ""));
        attColCheckIn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCheckIn() != null ? cd.getValue().getCheckIn().format(DTF) : "-"));
        attColCheckOut.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCheckOut() != null ? cd.getValue().getCheckOut().format(DTF) : "-"));
        attColStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));

        // Color-code status
        attColStatus.setCellFactory(col -> new TableCell<Attendance, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                switch (item) {
                    case "PRESENT":
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: 800;");
                        break;
                    case "ABSENT":
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 800;");
                        break;
                    case "LATE":
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: 800;");
                        break;
                    default:
                        setStyle("-fx-font-weight: 700;");
                }
            }
        });
        attTable.setItems(attList);

        attDatePicker.setValue(LocalDate.now());

        loadLookups();
        loadShifts();
        loadAttendance();
    }

    // ---------- Lookup helpers ----------
    private void loadLookups() {
        try {
            List<Employee> emps = employeeDao.findAll();
            empNames.clear();
            empNameToId.clear();
            empCombo.getItems().clear();
            for (Employee e : emps) {
                String label = e.getId() + " - "
                        + (e.getFullName() != null ? e.getFullName() : "Employee #" + e.getId());
                empNames.put(e.getId(), label);
                empNameToId.put(label, e.getId());
                empCombo.getItems().add(label);
            }
        } catch (SQLException e) {
            System.err.println("Load employees error: " + e.getMessage());
        }
        refreshShiftLookup();
    }

    private void refreshShiftLookup() {
        try {
            List<Shift> shifts = shiftDao.findAll();
            shiftNames.clear();
            shiftNameToId.clear();
            attShiftCombo.getItems().clear();
            for (Shift s : shifts) {
                String label = s.getId() + " - " + s.getName();
                shiftNames.put(s.getId(), label);
                shiftNameToId.put(label, s.getId());
                attShiftCombo.getItems().add(label);
            }
        } catch (SQLException e) {
            System.err.println("Load shifts error: " + e.getMessage());
        }
    }

    // ---------- Shift CRUD ----------
    private void loadShifts() {
        try {
            shiftList.setAll(shiftDao.findAll());
        } catch (SQLException e) {
            showAlert("Error loading shifts: " + e.getMessage());
        }
    }

    private void onShiftSelected(Shift s) {
        selectedShift = s;
        if (s == null)
            return;
        shiftNameField.setText(s.getName());
        shiftStartField.setText(s.getStartTime());
        shiftEndField.setText(s.getEndTime());
    }

    @FXML
    private void onAddShift() {
        shiftTable.getSelectionModel().clearSelection();
        selectedShift = null;
        onSaveShift();
    }

    @FXML
    private void onUpdateShift() {
        if (selectedShift == null) {
            showAlert("Select a shift to update.");
            return;
        }
        onSaveShift();
    }

    @FXML
    private void onSaveShift() {
        String name = shiftNameField.getText().trim();
        String start = shiftStartField.getText().trim();
        String end = shiftEndField.getText().trim();
        if (name.isEmpty() || start.isEmpty() || end.isEmpty()) {
            showAlert("All shift fields are required.");
            return;
        }
        try {
            if (selectedShift == null) {
                Shift s = new Shift();
                s.setName(name);
                s.setStartTime(start);
                s.setEndTime(end);
                shiftDao.insert(s);
                showInfo("Shift created.");
            } else {
                selectedShift.setName(name);
                selectedShift.setStartTime(start);
                selectedShift.setEndTime(end);
                shiftDao.update(selectedShift);
                showInfo("Shift updated.");
            }
            onClearShift();
            loadShifts();
            refreshShiftLookup();
        } catch (SQLException e) {
            showAlert("Save shift error: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteShift() {
        if (selectedShift == null) {
            showAlert("Select a shift to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete shift '" + selectedShift.getName() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    shiftDao.delete(selectedShift.getId());
                    showInfo("Shift deleted.");
                    onClearShift();
                    loadShifts();
                    refreshShiftLookup();
                } catch (SQLException e) {
                    showAlert("Delete shift error: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onClearShift() {
        selectedShift = null;
        shiftTable.getSelectionModel().clearSelection();
        shiftNameField.clear();
        shiftStartField.clear();
        shiftEndField.clear();
    }

    // ---------- Attendance ----------
    private void loadAttendance() {
        try {
            attList.setAll(attendanceDao.findAll());
        } catch (SQLException e) {
            showAlert("Error loading attendance: " + e.getMessage());
        }
    }

    @FXML
    private void onAssignAttendance() {
        String empLabel = empCombo.getValue();
        String shiftLabel = attShiftCombo.getValue();
        LocalDate date = attDatePicker.getValue();
        if (empLabel == null || shiftLabel == null || date == null) {
            showAlert("Select employee, shift, and date.");
            return;
        }
        Long empId = empNameToId.get(empLabel);
        Long shiftId = shiftNameToId.get(shiftLabel);
        if (empId == null || shiftId == null) {
            showAlert("Invalid selection.");
            return;
        }
        try {
            attendanceDao.upsertAssignment(empId, shiftId, date);
            showInfo("Attendance assigned.");
            loadAttendance();
        } catch (SQLException e) {
            showAlert("Assign error: " + e.getMessage());
        }
    }

    @FXML
    private void onFilterAttendance() {
        LocalDate from = attFromDate.getValue();
        LocalDate to = attToDate.getValue();
        if (from == null || to == null) {
            showAlert("Select both dates for filter.");
            return;
        }
        try {
            attList.setAll(attendanceDao.findByDateRange(from, to));
        } catch (SQLException e) {
            showAlert("Filter error: " + e.getMessage());
        }
    }

    @FXML
    private void onShowAllAttendance() {
        attFromDate.setValue(null);
        attToDate.setValue(null);
        loadAttendance();
    }

    @FXML
    private void onCheckIn() {
        Attendance sel = attTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select an attendance record.");
            return;
        }
        try {
            attendanceDao.checkIn(sel.getId());
            showInfo("Checked in.");
            loadAttendance();
        } catch (SQLException e) {
            showAlert("Check-in error: " + e.getMessage());
        }
    }

    @FXML
    private void onCheckOut() {
        Attendance sel = attTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select an attendance record.");
            return;
        }
        try {
            attendanceDao.checkOut(sel.getId());
            showInfo("Checked out.");
            loadAttendance();
        } catch (SQLException e) {
            showAlert("Check-out error: " + e.getMessage());
        }
    }

    @FXML
    private void onMarkAbsent() {
        Attendance sel = attTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select an attendance record.");
            return;
        }
        try {
            attendanceDao.updateStatus(sel.getId(), "ABSENT");
            showInfo("Marked absent.");
            loadAttendance();
        } catch (SQLException e) {
            showAlert("Status error: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteAttendance() {
        Attendance sel = attTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select an attendance record.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete attendance record #" + sel.getId() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    attendanceDao.delete(sel.getId());
                    showInfo("Record deleted.");
                    loadAttendance();
                } catch (SQLException e) {
                    showAlert("Delete error: " + e.getMessage());
                }
            }
        });
    }

    // ---------- Utilities ----------
    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
