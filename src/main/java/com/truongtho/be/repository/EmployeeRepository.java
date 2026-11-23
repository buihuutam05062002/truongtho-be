package com.truongtho.be.repository;

import com.truongtho.be.entity.Employee;
import com.truongtho.be.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    @EntityGraph(attributePaths = {"users"})
    @Query("""
            select e from Employee e
            where (:keyword is null
                   or lower(e.users.fullName) like :keyword
                   or lower(e.users.username) like :keyword
                   or lower(e.users.email) like :keyword
                   or e.users.phoneNumber like :keyword)
              and (:status is null or e.employeeStatus = :status)
            """)
    Page<Employee> search(@Param("keyword") String keyword,
                          @Param("status") EmployeeStatus status,
                          Pageable pageable);

    @Query("select e.employeeStatus, count(e) from Employee e group by e.employeeStatus")
    List<Object[]> countByStatus();
}
