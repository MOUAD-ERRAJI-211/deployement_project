package orthoproconnect.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orthoproconnect.model.Appointment;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Find appointments by patient name
    List<Appointment> findByPatientNameContainingIgnoreCase(String patientName);

    // Find appointments by phone number
    List<Appointment> findByPatientPhone(String patientPhone);

    // Find appointments by date
    List<Appointment> findByAppointmentDate(LocalDate date);

    // Find appointments by date and status
    List<Appointment> findByAppointmentDateAndStatus(LocalDate date, Appointment.AppointmentStatus status);

    // Find appointments by department
    List<Appointment> findByDepartment(String department);

    // Find appointments by status
    List<Appointment> findByStatus(Appointment.AppointmentStatus status);
}