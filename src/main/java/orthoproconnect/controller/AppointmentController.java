package orthoproconnect.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import orthoproconnect.model.Appointment;
import orthoproconnect.repository.AppointmentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    // Get all appointments
    @GetMapping
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // Get appointment by ID
    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable Long id) {
        Optional<Appointment> appointment = appointmentRepository.findById(id);
        return appointment.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create a new appointment
    @PostMapping
    public ResponseEntity<Appointment> createAppointment(@RequestBody Appointment appointment) {
        // Set default status to SCHEDULED if not provided
        if (appointment.getStatus() == null) {
            appointment.setStatus(Appointment.AppointmentStatus.SCHEDULED);
        }
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return new ResponseEntity<>(savedAppointment, HttpStatus.CREATED);
    }

    // Update an appointment
    @PutMapping("/{id}")
    public ResponseEntity<Appointment> updateAppointment(@PathVariable Long id, @RequestBody Appointment appointmentDetails) {
        Optional<Appointment> optionalAppointment = appointmentRepository.findById(id);

        if (optionalAppointment.isPresent()) {
            Appointment appointment = optionalAppointment.get();

            appointment.setPatientName(appointmentDetails.getPatientName());
            appointment.setPatientPhone(appointmentDetails.getPatientPhone());
            appointment.setPatientEmail(appointmentDetails.getPatientEmail());
            appointment.setAppointmentDate(appointmentDetails.getAppointmentDate());
            appointment.setAppointmentTime(appointmentDetails.getAppointmentTime());
            appointment.setDepartment(appointmentDetails.getDepartment());
            appointment.setReason(appointmentDetails.getReason());
            appointment.setStatus(appointmentDetails.getStatus());
            appointment.setNotes(appointmentDetails.getNotes());

            return ResponseEntity.ok(appointmentRepository.save(appointment));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete an appointment
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        Optional<Appointment> appointment = appointmentRepository.findById(id);

        if (appointment.isPresent()) {
            appointmentRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Get appointments by date
    @GetMapping("/date/{date}")
    public List<Appointment> getAppointmentsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentRepository.findByAppointmentDate(date);
    }

    // Get appointments by patient phone
    @GetMapping("/phone/{phone}")
    public List<Appointment> getAppointmentsByPhone(@PathVariable String phone) {
        return appointmentRepository.findByPatientPhone(phone);
    }

    // Get appointments by department
    @GetMapping("/department/{department}")
    public List<Appointment> getAppointmentsByDepartment(@PathVariable String department) {
        return appointmentRepository.findByDepartment(department);
    }

    // Get appointments by status
    @GetMapping("/status/{status}")
    public List<Appointment> getAppointmentsByStatus(@PathVariable Appointment.AppointmentStatus status) {
        return appointmentRepository.findByStatus(status);
    }
}