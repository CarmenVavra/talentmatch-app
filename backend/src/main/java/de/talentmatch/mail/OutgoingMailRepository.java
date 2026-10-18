package de.talentmatch.mail;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutgoingMailRepository extends JpaRepository<OutgoingMail, Long> {

    List<OutgoingMail> findAllByOrderByCreatedAtDescIdDesc();

    List<OutgoingMail> findByApplicationIdOrderByCreatedAtDescIdDesc(Long applicationId);

    @Modifying
    @Query("delete from OutgoingMail m where m.applicationId in :applicationIds")
    void deleteByApplicationIds(@Param("applicationIds") Collection<Long> applicationIds);
}
