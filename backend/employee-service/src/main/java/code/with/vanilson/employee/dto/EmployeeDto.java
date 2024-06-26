package code.with.vanilson.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EmployeeDto {
    private long employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String departmentCode;

    public EmployeeDto() {
        //default constructor
    }


}
