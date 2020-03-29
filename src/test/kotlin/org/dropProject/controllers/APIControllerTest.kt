package org.dropProject.controllers

import org.dropProject.TestsHelper
import org.dropProject.dao.Assignment
import org.dropProject.dao.AssignmentACL
import org.dropProject.forms.SubmissionMethod
import org.dropProject.repository.AssigneeRepository
import org.dropProject.repository.AssignmentACLRepository
import org.dropProject.repository.AssignmentRepository
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.io.File

@RunWith(SpringRunner::class)
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(locations=["classpath:drop-project-test.properties"])
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class APIControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var assigneeRepository: AssigneeRepository

    @Autowired
    lateinit var assignmentRepository: AssignmentRepository

    @Autowired
    lateinit var assignmentACLRepository: AssignmentACLRepository

    @Autowired
    lateinit var testsHelper: TestsHelper

    @Value("\${assignments.rootLocation}")
    val assignmentsRootLocation: String = ""

    @Value("\${mavenizedProjects.rootLocation}")
    val mavenizedProjectsRootLocation: String = ""

    @Value("\${storage.rootLocation}")
    val submissionsRootLocation: String = ""

    val STUDENT_1 = User("student1", "", mutableListOf(SimpleGrantedAuthority("ROLE_STUDENT")))
    val TEACHER_1 = User("teacher1", "", mutableListOf(SimpleGrantedAuthority("ROLE_TEACHER")))


    @Test
    @DirtiesContext
    fun test_00_getStudentAssignmentList() {
        try {

            if(File(assignmentsRootLocation, "dummyAssignment1").exists()) {
                File(assignmentsRootLocation, "dummyAssignment1").deleteRecursively()
            }

            testsHelper.createAndSetupAssignment(mvc, assignmentRepository, "dummyAssignment1", "Dummy Assignment",
                    "org.dummy",
                    "UPLOAD", "git@github.com:palves-ulht/sampleJavaAssignment.git",
                    assignees = "student1")


            this.mvc.perform(get("/api/v1/assignmentList")
                    .with(user(STUDENT_1))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    // .andDo(MockMvcResultHandlers.print())
                    .andExpect(jsonPath("$[0].assignmentId", Matchers.equalTo("dummyAssignment1")))

        } finally {

            if(File(assignmentsRootLocation, "dummyAssignment1").exists()) {
                File(assignmentsRootLocation, "dummyAssignment1").deleteRecursively()
            }
        }

    }
}