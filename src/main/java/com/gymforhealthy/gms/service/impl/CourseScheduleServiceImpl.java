package com.gymforhealthy.gms.service.impl;

import com.gymforhealthy.gms.dto.requestDto.CourseScheduleRequestDto;
import com.gymforhealthy.gms.dto.responseDto.CourseScheduleResponseDto;
import com.gymforhealthy.gms.entity.Course;
import com.gymforhealthy.gms.entity.CourseSchedule;
import com.gymforhealthy.gms.entity.User;
import com.gymforhealthy.gms.exception.ResourceNotFoundException;
import com.gymforhealthy.gms.repository.CourseRepository;
import com.gymforhealthy.gms.repository.CourseScheduleRepository;
import com.gymforhealthy.gms.repository.UserRepository;
import com.gymforhealthy.gms.service.CourseScheduleService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@AllArgsConstructor
public class CourseScheduleServiceImpl implements CourseScheduleService {

    private final CourseScheduleRepository courseScheduleRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Override
    public CourseScheduleResponseDto saveCourseSchedule(CourseScheduleRequestDto courseScheduleRequestDto) {

        // Çakışma kontrolü
        checkTimeConflict(
                courseScheduleRequestDto.getCourseDate(),
                courseScheduleRequestDto.getStartTime(),
                courseScheduleRequestDto.getEndTime(),
                courseScheduleRequestDto.getTrainerId()
        );

        // Eğitmeni bul
        User user = userRepository.findById(courseScheduleRequestDto.getTrainerId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found with id: " + courseScheduleRequestDto.getTrainerId()));

        if (!user.getRole().getName().equalsIgnoreCase("TRAINER")) {
            throw new IllegalArgumentException("Only users with TRAINER role can schedule course.");
        }

        // Kursu bul
        Course course = courseRepository.findById(courseScheduleRequestDto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseScheduleRequestDto.getCourseId()));

        // Manuel mapleme
        CourseSchedule courseSchedule = new CourseSchedule();
        courseSchedule.setId(null);
        courseSchedule.setCourseDate(courseScheduleRequestDto.getCourseDate());
        courseSchedule.setStartTime(courseScheduleRequestDto.getStartTime());
        courseSchedule.setEndTime(courseScheduleRequestDto.getEndTime());
        courseSchedule.setTrainer(user);
        courseSchedule.setCourse(course);

        courseSchedule = courseScheduleRepository.save(courseSchedule);
        return convertToCourseScheduleResponseDto(courseSchedule);
    }

    @Override
    public CourseScheduleResponseDto updateCourseSchedule(Long id, CourseScheduleRequestDto courseScheduleRequestDto) {

        CourseSchedule existingSchedule = courseScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course schedule not found with id: " + id));

        // Çakışma kontrolü (kendisi hariç)
        checkTimeConflictForUpdate(
                courseScheduleRequestDto.getCourseDate(),
                courseScheduleRequestDto.getStartTime(),
                courseScheduleRequestDto.getEndTime(),
                courseScheduleRequestDto.getTrainerId(),
                id
        );

        // Eğitmeni bul
        User user = userRepository.findById(courseScheduleRequestDto.getTrainerId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found with id: " + courseScheduleRequestDto.getTrainerId()));

        if (!user.getRole().getName().equalsIgnoreCase("TRAINER")) {
            throw new IllegalArgumentException("Only users with TRAINER role can schedule course.");
        }

        // Kursu bul
        Course course = courseRepository.findById(courseScheduleRequestDto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseScheduleRequestDto.getCourseId()));

        // Güncelle (manuel)
        existingSchedule.setCourseDate(courseScheduleRequestDto.getCourseDate());
        existingSchedule.setStartTime(courseScheduleRequestDto.getStartTime());
        existingSchedule.setEndTime(courseScheduleRequestDto.getEndTime());
        existingSchedule.setTrainer(user);
        existingSchedule.setCourse(course);

        existingSchedule = courseScheduleRepository.save(existingSchedule);
        return convertToCourseScheduleResponseDto(existingSchedule);
    }

    private void checkTimeConflict(LocalDate courseDate, LocalTime newStart, LocalTime newEnd, Long trainerId) {
        List<CourseSchedule> existingSchedules =
                courseScheduleRepository.findByCourseDateAndTrainerId(courseDate, trainerId);

        for (CourseSchedule schedule : existingSchedules) {
            LocalTime existStart = schedule.getStartTime();
            LocalTime existEnd = schedule.getEndTime();

            boolean isConflict = newStart.isBefore(existEnd) && newEnd.isAfter(existStart);

            if (isConflict) {
                throw new IllegalArgumentException(
                        "Time conflict detected: Trainer already has a course scheduled from "
                                + existStart + " to " + existEnd);
            }
        }
    }

    private void checkTimeConflictForUpdate(LocalDate courseDate, LocalTime newStart, LocalTime newEnd, Long trainerId, Long currentScheduleId) {
        List<CourseSchedule> existingSchedules =
                courseScheduleRepository.findByCourseDateAndTrainerId(courseDate, trainerId);

        for (CourseSchedule schedule : existingSchedules) {
            if (schedule.getId().equals(currentScheduleId)) {
                continue;
            }

            LocalTime existStart = schedule.getStartTime();
            LocalTime existEnd = schedule.getEndTime();

            boolean isConflict = newStart.isBefore(existEnd) && newEnd.isAfter(existStart);

            if (isConflict) {
                throw new IllegalArgumentException(
                        "Time conflict detected: Trainer already has a course scheduled from "
                                + existStart + " to " + existEnd);
            }
        }
    }

    @Override
    public List<CourseScheduleResponseDto> findAllCourseSchedule() {
        return courseScheduleRepository.findAll().stream()
                .map(this::convertToCourseScheduleResponseDto)
                .toList();
    }

    @Override
    public CourseScheduleResponseDto findCourseScheduleById(Long id) {
        CourseSchedule courseSchedule = courseScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CourseSchedule not found with id " + id));
        return convertToCourseScheduleResponseDto(courseSchedule);
    }

    @Override
    public void deleteCourseSchedule(Long id) {
        CourseSchedule courseSchedule = courseScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CourseSchedule not found with id " + id));
        courseScheduleRepository.delete(courseSchedule);
    }

    private CourseScheduleResponseDto convertToCourseScheduleResponseDto(CourseSchedule courseSchedule) {
        CourseScheduleResponseDto courseScheduleResponseDto = modelMapper.map(courseSchedule, CourseScheduleResponseDto.class);
        courseScheduleResponseDto.setTrainerId(courseSchedule.getTrainer().getId());
        courseScheduleResponseDto.setCourseId(courseSchedule.getCourse().getId());
        return courseScheduleResponseDto;
    }
}