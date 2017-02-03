/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016  Khartec Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.khartec.waltz.jobs;

import com.khartec.waltz.common.SetUtilities;
import com.khartec.waltz.model.*;
import com.khartec.waltz.model.survey.*;
import com.khartec.waltz.service.DIConfiguration;
import com.khartec.waltz.service.survey.SurveyRunService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;


public class SurveyHarness {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(DIConfiguration.class);


        IdSelectionOptions idSelectionOptions = ImmutableIdSelectionOptions.builder()
                .entityReference(ImmutableEntityReference.mkRef(EntityKind.APP_GROUP, 1))
                .scope(HierarchyQueryScope.EXACT)
                .build();

        SurveyRunCreateCommand surveyRunCreateCommand = ImmutableSurveyRunCreateCommand.builder()
                .surveyTemplateId(1L)
                .name("Q1 Quality Survey")
                .selectionOptions(idSelectionOptions)
                .issuanceKind(SurveyIssuanceKind.INDIVIDUAL)
                .involvementKindIds(SetUtilities.fromCollection(
                        LongStream.range(1, 5).mapToObj(Long::valueOf)
                        .collect(toList())))
                .contactEmail("jack.livingston12@gmail.com")
                .build();

        SurveyRunService surveyRunService = ctx.getBean(SurveyRunService.class);

        String userName = "livingston@mail.com";
        long surveyRunId = surveyRunService.createSurveyRun(userName, surveyRunCreateCommand).id().get();

        List<SurveyInstanceRecipient> surveyInstanceRecipients = surveyRunService.generateSurveyInstanceRecipients(surveyRunId);

        surveyInstanceRecipients.forEach(r -> System.out.println(
                r.surveyInstance().surveyEntity().name().get()
                + " => "
                + r.person().email()));

        System.out.println("Generated recipients count: " + surveyInstanceRecipients.size());

        surveyRunService.createSurveyInstancesAndRecipients(surveyRunId, surveyInstanceRecipients.subList(0, 5));

        ImmutableSurveyRunChangeCommand surveyRunChangeCommand = ImmutableSurveyRunChangeCommand.builder()
                .surveyTemplateId(1L)
                .name("Q2 Quality Survey")
                .selectionOptions(idSelectionOptions)
                .issuanceKind(SurveyIssuanceKind.GROUP)
                .involvementKindIds(SetUtilities.fromCollection(
                        LongStream.range(3, 7).mapToObj(Long::valueOf)
                                .collect(toList())))
                .contactEmail("jack.livingston12@gmail.com")
                .build();

        // update survey run
        surveyRunService.updateSurveyRun(userName, surveyRunId, surveyRunChangeCommand);

        List<SurveyInstanceRecipient> updatedSurveyInstanceRecipients = surveyRunService.generateSurveyInstanceRecipients(surveyRunId);
        System.out.println("Updated Generated recipients count: " + updatedSurveyInstanceRecipients.size());

        // generate the instances and recipients again
        surveyRunService.createSurveyInstancesAndRecipients(surveyRunId, Collections.emptyList());

        // finally publish
        surveyRunService.updateSurveyRunStatus(userName, surveyRunId, SurveyRunStatus.ISSUED);
    }


}