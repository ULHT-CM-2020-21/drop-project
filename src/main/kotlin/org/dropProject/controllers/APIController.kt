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

import org.springframework.web.bind.annotation.*
import org.dropProject.dao.Assignee
import org.dropProject.extensions.realName
import org.dropProject.repository.*
import java.security.Principal


@RestController
@RequestMapping("/api/v1")
class APIController(val assigneeRepository: AssigneeRepository) {


    /**
     * Returns list of assignments given to a student
     */
    @GetMapping(value = ["/assignmentList"])
    fun getStudentAssignmentList(principal: Principal) : List<Assignee> {
        return assigneeRepository.findByAuthorUserId(principal.realName())
    }


}