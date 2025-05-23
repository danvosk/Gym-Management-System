
package com.gymforhealthy.gms.repository;

import com.gymforhealthy.gms.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long>{
}