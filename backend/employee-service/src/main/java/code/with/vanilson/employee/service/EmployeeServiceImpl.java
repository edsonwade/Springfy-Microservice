package code.with.vanilson.employee.service;

import code.with.vanilson.employee.api.client.APIClient;
import code.with.vanilson.employee.dto.APIResponseDto;
import code.with.vanilson.employee.dto.DepartmentDto;
import code.with.vanilson.employee.dto.EmployeeDto;
import code.with.vanilson.employee.exception.EmployeeBadRequestException;
import code.with.vanilson.employee.exception.EmployeeNotFoundException;
import code.with.vanilson.employee.exception.EmployeeWithEmailAlreadyExistsException;
import code.with.vanilson.employee.mapper.EmployeeMapper;
import code.with.vanilson.employee.model.Employee;
import code.with.vanilson.employee.repository.EmployeeRepository;
import code.with.vanilson.util.MessageProvider;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    private final EmployeeMapper employeeMapper;

    private final APIClient apiClient;

    private static final String EMPLOYEE_NOT_FOUND_MESSAGE = "employee.error.not_found";

    public EmployeeServiceImpl(APIClient apiClient, EmployeeRepository employeeRepository,
                               EmployeeMapper employeeMapper) {
        this.apiClient = apiClient;
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
    }

    /**
     * Retrieves all employees and maps them to DTOs.
     *
     * @return List of EmployeeDto objects representing all employees.
     */
    @Override
    public List<EmployeeDto> findAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        return employeeMapper.mapEmployeeListToDto(employees);
    }

    /**
     * Finds an employee by its ID.
     *
     * @param employeeId The ID of the employee to find.
     * @return The DTO representing the found employee.
     * @throws EmployeeNotFoundException   if the employee with the given ID is not found.
     * @throws EmployeeBadRequestException if the provided employee ID is not valid.
     */
    @Override
    public EmployeeDto findEmployeeById(long employeeId) {
        if (employeeId <= 0) {
            var errorMessage = MessageProvider.getMessage("employee.error.bad_request", employeeId);
            throw new EmployeeBadRequestException(errorMessage);
        }
        Optional<Employee> employee = employeeRepository.findById(employeeId);
        return employee.map(employeeMapper::mapToEmployeeDto)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        MessageFormat.format(MessageProvider.getMessage(EMPLOYEE_NOT_FOUND_MESSAGE), employeeId)));
    }

    /**
     * Finds an employee by its Email.
     *
     * @param employeeEmail The email of the employee to find.
     * @return The DTO representing the found employee.
     * @throws EmployeeNotFoundException if the employee with the given EMAIL is not found.
     */
    @Override
    public EmployeeDto findEmployeeByEmail(String employeeEmail) {
        Employee employee = employeeRepository.findEmployeeByEmail(employeeEmail);
        if (employee == null) {
            throw new EmployeeNotFoundException(
                    MessageFormat.format(MessageProvider.getMessage("employee.error.email.not_found"), employeeEmail));
        }
        return employeeMapper.mapToEmployeeDto(employee);
    }

    /**
     * Retrieves an employee and their associated department information by their ID.
     *
     * @param employeeId The ID of the employee to retrieve.
     * @return An APIResponseDto containing the DTOs representing the employee and their department.
     * @throws EmployeeNotFoundException if no employee is found with the given ID.
     */
    @Override
    public APIResponseDto getEmployeeWithDepartmentById(Long employeeId) {
        // Retrieve the employee from the repository
        Optional<Employee> optionalEmployee = employeeRepository.findById(employeeId);
        if (optionalEmployee.isEmpty()) {
            throw new EmployeeNotFoundException(
                    MessageFormat.format(MessageProvider.getMessage(EMPLOYEE_NOT_FOUND_MESSAGE), employeeId));
        }
        Employee employee = optionalEmployee.get();

        // Retrieve department information using apiClient
        DepartmentDto departmentDto = apiClient.getDepartment(employee.getDepartmentCode());

        // Create and populate the response DTO
        return createAPIResponseDto(employee, departmentDto);
    }

    /**
     * Saves an employee based on the provided EmployeeDto.
     *
     * @param employeeDto The EmployeeDto containing employee information to be saved.
     * @return The saved EmployeeDto after persisting the data.
     * @throws EmployeeWithEmailAlreadyExistsException If an employee with the specified email already exists.
     */
    @Override
    public EmployeeDto saveEmployeeDto(EmployeeDto employeeDto) {
        validateEmailAndThrowException(employeeDto.getEmail());
        Employee employee = employeeMapper.mapToEmployeeEntity(employeeDto);
        Employee savedEmployee = employeeRepository.save(employee);
        return employeeMapper.mapToEmployeeDto(savedEmployee);
    }

    /**
     * Updates an employee based on the provided EmployeeDto.
     *
     * @param employeeDto The EmployeeDto containing updated employee information.
     * @return The updated EmployeeDto after saving the changes.
     * @throws EmployeeNotFoundException               If the employee with the specified ID does not exist.
     * @throws EmployeeWithEmailAlreadyExistsException If an employee with the specified email already exists.
     */
    @Override
    public EmployeeDto updateEmployeeDto(EmployeeDto employeeDto) {
        validateEmployeeId(employeeDto.getEmployeeId());
        validateEmailAndThrowException(employeeDto.getEmail());
        Employee employee = employeeMapper.mapToEmployeeEntity(employeeDto);
        Employee updatedEmployee = employeeRepository.save(employee);
        return employeeMapper.mapToEmployeeDto(updatedEmployee);
    }

    /**
     * Deletes an employee by its ID.
     *
     * @param employeeId The ID of the employee to delete.
     * @throws EmployeeNotFoundException If the employee with the specified ID does not exist.
     */
    @Override
    public void deleteEmployee(long employeeId) {
        validateEmployeeId(employeeId);
        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException(
                    MessageFormat.format(MessageProvider.getMessage(EMPLOYEE_NOT_FOUND_MESSAGE), employeeId));
        }
        employeeRepository.deleteById(employeeId);
    }

    /**
     * Validates whether an employee with the given ID exists.
     *
     * @param employeeId The ID of the employee to check.
     * @throws EmployeeNotFoundException   If the employee with the specified ID does not exist.
     * @throws EmployeeBadRequestException If the provided employee ID is not valid.
     */
    private void validateEmployeeId(long employeeId) {
        if (employeeId <= 0) {
            String errorMessage = MessageProvider.getMessage("employee.error.bad_request", employeeId);
            throw new EmployeeBadRequestException(errorMessage);
        }
    }

    /**
     * Validates whether an employee with the given email already exists.
     *
     * @param email The email address to check.
     * @throws EmployeeWithEmailAlreadyExistsException If an employee with the specified email already exists.
     */
    private void validateEmailAndThrowException(String email) {
        if (employeeRepository.existsByEmail(email)) {
            throw new EmployeeWithEmailAlreadyExistsException(
                    MessageFormat.format(MessageProvider.getMessage("employee.error.email.conflict_request"), email));
        }
    }

    /**
     * Creates an APIResponseDto from the given employee and department information.
     *
     * @param employee      The employee to include in the response.
     * @param departmentDto The department information to include in the response.
     * @return An APIResponseDto containing the employee and department DTOs.
     */
    private static APIResponseDto createAPIResponseDto(Employee employee, DepartmentDto departmentDto) {
        EmployeeDto employeeDto = new EmployeeDto(
                employee.getEmployeeId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartmentCode()
        );

        // Create and populate the response DTO
        APIResponseDto apiResponseDto = new APIResponseDto();
        apiResponseDto.setEmployeeDto(employeeDto);
        apiResponseDto.setDepartmentDto(departmentDto);
        return apiResponseDto;
    }

}
