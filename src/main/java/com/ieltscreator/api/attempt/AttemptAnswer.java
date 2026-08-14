package com.ieltscreator.api.attempt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attempt_answer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AttemptAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "attempt_id", nullable = false)
  private UUID attemptId;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Column(name = "user_answer_text", columnDefinition = "text")
  private String userAnswerText;

  @Column(name = "is_correct")
  private Boolean isCorrect;

  /** 採点時点の正解のスナップショット（不変記録）。提出前はnull。 */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "correct_answer_snapshot", columnDefinition = "jsonb")
  private String correctAnswerSnapshot;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AttemptAnswer other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
