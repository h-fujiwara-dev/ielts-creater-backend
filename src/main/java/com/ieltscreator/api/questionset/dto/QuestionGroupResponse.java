package com.ieltscreator.api.questionset.dto;

import com.ieltscreator.api.questionset.QuestionFormatType;
import java.util.List;

public record QuestionGroupResponse(
    QuestionFormatType formatType, String instructions, List<QuestionResponse> questions) {}
