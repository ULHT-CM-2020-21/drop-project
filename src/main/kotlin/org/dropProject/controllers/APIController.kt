/*-
 * ========================LICENSE_START=================================
 * DropProject
 * %%
 * Copyright (C) 2019 - 2020 Pedro Alves
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
/*-
 * Plugin Drop Project
 * 
 * Copyright (C) 2019 Yash Jahit
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dropProject.controllers

import org.apache.poi.ss.formula.functions.T
import org.springframework.web.bind.annotation.*
import org.dropProject.dao.Language
import org.dropProject.dao.Submission
import org.dropProject.dao.SubmissionStatus
import org.dropProject.data.JUnitSummary
import org.dropProject.data.TestType
import org.dropProject.extensions.formatDefault
import org.dropProject.extensions.realName
import org.dropProject.repository.*
import org.dropProject.services.AssignmentTeacherFiles
import org.dropProject.services.BuildReportBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.ui.ModelMap
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.security.Principal
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.security.auth.message.callback.SecretKeyCallback
import javax.servlet.http.HttpServletRequest

val formatter = DateTimeFormatter.ofPattern("dd MMM YYYY HH:mm")
val dateFormater = SimpleDateFormat("dd MMM YYYY HH:mm")

@RestController
@RequestMapping("/api/v1")
class APIController(val assigneeRepository: AssigneeRepository,
                    val assignmentRepository: AssignmentRepository,
                    val assignmentTeacherFiles: AssignmentTeacherFiles,
                    val submissionRepository: SubmissionRepository,
                    val buildReportRepository: BuildReportRepository,
                    val reportController: SubmissionReportRepository, val buildReportBuilder: BuildReportBuilder) {


    /**
     * Returns list of assignments given to a student,
     * containing information such as:
     *
     * {assingmentId, language, dueDate}
     */
    @GetMapping(value = ["/assignmentList"])
    fun getStudentAssignmentList(principal: Principal): List<AssignmentInformation> {
        val listAssigne = assigneeRepository.findByAuthorUserId(principal.realName())

        val listAssigment = listAssigne.map { assignee ->
            assignmentRepository.findAssignmentById(assignee.assignmentId)
        }

        return listAssigment.map { assignment ->
            val html = assignmentTeacherFiles.getHtmlInstructionsFragment(assignment)
            html.replace("\"", "\\\"")
            AssignmentInformation(assignment.id, assignment.language, assignment.dueDate?.format(formatter), html)
        }
    }

    @GetMapping(value = ["/submissionsList/{assignmentId}"])
    fun getStudentAssignmentSubmissions(principal: Principal, @PathVariable assignmentId: String): List<SubmissionInformation> {

            val submissionsList = submissionRepository.findBySubmitterUserIdAndAssignmentId(principal.realName(), assignmentId)

            return submissionsList.map { submission ->
                val assignment = assignmentRepository.findById(submission.assignmentId).orElse(null)
                val mavenizedProjectFolder = assignmentTeacherFiles.getProjectFolderAsFile(submission,
                        submission.getStatus() == SubmissionStatus.VALIDATED_REBUILT)

                if (submission.buildReportId != null) {
                    val buildReportDB = buildReportRepository.getOne(submission.buildReportId!!)
                    val buildReport = buildReportBuilder.build(buildReportDB.buildReport.split("\n"),
                            mavenizedProjectFolder.absolutePath, assignment, submission)

                    val date = dateFormater.format(submission.submissionDate)

                    SubmissionInformation(
                            submission.id,
                            date,
                            buildReport.jUnitErrors(),
                            buildReport.junitSummary(),
                            submission.assignmentId)
                } else {
                    SubmissionInformation(
                            submission.id,
                            dateFormater.format(submission.submissionDate),
                            "",
                            "",
                            submission.assignmentId)
                }
            }
    }

}

data class AssignmentInformation(val id: String,
                                 val language: Language,
                                 val date: String?,
                                 val html: String)

data class SubmissionInformation(val submissionId: Long,
                                 val submissionDate: String?,
                                 val report: String?,
                                 val summary: String?,
                                 val assignmentId: String)
