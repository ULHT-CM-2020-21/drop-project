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
import org.dropProject.dao.ProjectGroup
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
            AssignmentInformation(assignment.id,
                    assignment.name,
                    assignment.language,
                    assignment.dueDate.toString(),assignment.numSubmissions,html,assignment.lastSubmissionDate,assignment.active)
        }
    }

    @GetMapping(value = ["/submissionsList/{assignmentId}"])
    fun getStudentAssignmentSubmissions(principal: Principal, @PathVariable assignmentId: String): List<SubmissionInformation> {

            val submissionsList = submissionRepository.findByAssignmentId(assignmentId)
            //submissionRepository.findBySubmitterUserIdAndAssignmentId
            println("ola")
            return submissionsList.map { submission ->
                val assignment = assignmentRepository.findById(submission.assignmentId).orElse(null)
                val mavenizedProjectFolder = assignmentTeacherFiles.getProjectFolderAsFile(submission,
                        submission.getStatus() == SubmissionStatus.VALIDATED_REBUILT)
                println("ole")
                if (submission.buildReportId != null) {
                    val buildReportDB = buildReportRepository.getOne(submission.buildReportId!!)
                    println("ola2 ")
                    val buildReport = buildReportBuilder.build(buildReportDB.buildReport.split("\n"),
                            mavenizedProjectFolder.absolutePath, assignment, submission)
                    println("ola3 +" + submission.submissionDate)

                    val date = dateFormater.format(submission.submissionDate)
                    try{
                        println(">>>> " )
                        println(buildReport)
                    }catch (e: Exception){
                        println("tamos aqui" +  e.message)
                    }

                    SubmissionInformation(
                            submission.id,
                            submission.group.id,
                            submission.group.authors.toString(),
                            submission.submitterUserId,
                            date,
                            buildReport.jUnitErrors(),
                            buildReport.junitSummary(),
                            submission.getStatus().toString(),
                            submission.structureErrors,
                            submission.teacherTests?.toStr(),
                            submission.hiddenTests?.toStr(),
                            submission.studentTests?.toStr(),
                            submission.ellapsed,
                            submission.coverage,
                            submission.markedAsFinal,
                            submission.assignmentId)
                            } else {
                            SubmissionInformation(
                            submission.id,
                            submission.group.id,
                            submission.group.authors.toString(),
                            submission.submitterUserId,
                            dateFormater.format(submission.submissionDate),
                            "",
                            "A submissão ainda não foi validada. Aguarde...",
                            submission.getStatus().toString(),
                            submission.structureErrors,

                            submission.teacherTests?.toStr(),
                            submission.hiddenTests?.toStr(),
                            submission.studentTests?.toStr(),


                            submission.ellapsed,
                            submission.coverage,
                            submission.markedAsFinal,
                            submission.assignmentId)
                            }
                            }

}
@GetMapping(value = ["/teacherAssignmentList"])
fun getTeacherAssignmentList(principal: Principal): List<AssignmentInformation> {
var result = mutableListOf<AssignmentInformation>()
for(assignment in assignmentRepository.findAll()) {
result.add(AssignmentInformation(assignment.id,
assignment.name,
assignment.language,
assignment.dueDate.toString(),assignment.numSubmissions,assignmentTeacherFiles.getHtmlInstructionsFragment(assignment).replace("\"", "\\\"") ,assignment.lastSubmissionDate,assignment.active))
}
return result;
}

}

data class AssignmentInformation(val id: String,
        val name: String,
        val language: Language,
        val date: String?,
        val numSubmissions: Int,
        val html: String,
        val lastSubmissionDate: Date?,
        val active: Boolean)

        data class SubmissionInformation(val submissionId: Long,
        var idGroup : Long,
        var groupAuthors: String,
        val submitterUserId : String,
        val submissionDate: String?,
        val report: String?,
        val summary: String?,
        val status: String,
        val structureErrors: String?,
        val teacherTests: String?,
        val hiddenTests: String?,
        val studentTests: String?,
        val elapsed: BigDecimal?,
        val coverage: Int?,
        val markedAsFinal: Boolean,
        val assignmentId: String)
