package com.ieltscreator.api.questionset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Question {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_group_id", nullable = false)
  private QuestionGroup questionGroup;

  @Column(name = "prompt_text", nullable = false, columnDefinition = "text")
  private String promptText;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String metadata;

  /** 正解（形式により構造が異なるJSON。TFNG/MATCHING_HEADINGSは文字列、MCQは文字列配列）。 */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "correct_answer_key", nullable = false, columnDefinition = "jsonb")
  private String correctAnswerKey;

  @Column(columnDefinition = "text")
  private String explanation;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Question other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
