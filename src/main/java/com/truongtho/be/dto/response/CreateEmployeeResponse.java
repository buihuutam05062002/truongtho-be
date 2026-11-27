package com.truongtho.be.dto.response;

/**
 * @param tempPassword null when an existing account was promoted (it keeps its own password)
 * @param promoted     true when the email already belonged to a user and was upgraded to staff
 */
public record CreateEmployeeResponse(EmployeeResponse employee, String tempPassword, boolean promoted) {
}
