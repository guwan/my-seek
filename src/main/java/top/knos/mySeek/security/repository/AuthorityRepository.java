/*
 * Copyright 2013-2014 the original author or authors.
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
package top.knos.mySeek.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import top.knos.mySeek.security.domain.Authority;

import java.io.Serializable;
import java.util.List;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, Integer> {
    Authority findByAuthority(String authority);

    Authority findByName(String name);

    @Query(value = """
            select distinct authority
            from sso_resource r ,sso_authority_resources ar, sso_authority a  
            where r.path = :url 
            and r.id = ar.resources_id
            and ar.authority_id = a.id
        """,nativeQuery = true)
    List<String> findRolesByUrl(String url);

    List<Authority> findByAuthorityIn(List<? extends Serializable> name);

//	@Query("delete from sso_user_authority where user_id in (select id from sso_user where username = ?1)")
//	int deleteByUsername(String username);


}
