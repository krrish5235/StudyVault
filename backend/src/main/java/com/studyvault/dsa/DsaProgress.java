package com.studyvault.dsa;

import com.studyvault.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "dsa_progress")
@IdClass(DsaProgress.Pk.class)
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class DsaProgress {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(nullable = false)
    private boolean done = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Composite primary key matching (user, questionId) - names must mirror the @Id properties. */
    public static class Pk implements Serializable {
        private UUID user;
        private Integer questionId;

        public Pk() {
        }

        public Pk(UUID user, Integer questionId) {
            this.user = user;
            this.questionId = questionId;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pk that)) return false;
            return Objects.equals(user, that.user) && Objects.equals(questionId, that.questionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user, questionId);
        }
    }
}
