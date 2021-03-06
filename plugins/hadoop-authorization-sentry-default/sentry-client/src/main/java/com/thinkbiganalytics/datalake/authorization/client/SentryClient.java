package com.thinkbiganalytics.datalake.authorization.client;

/*-
 * #%L
 * thinkbig-sentry-client
 * %%
 * Copyright (C) 2017 ThinkBig Analytics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.thinkbiganalytics.datalake.authorization.hdfs.HDFSUtil;
import com.thinkbiganalytics.datalake.authorization.model.HadoopAuthorizationGroup;
import com.thinkbiganalytics.datalake.authorization.model.SentryGroup;
import com.thinkbiganalytics.datalake.authorization.model.SentrySearchPolicy;
import com.thinkbiganalytics.datalake.authorization.model.SentrySearchPolicyMapper;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */

public class SentryClient {

    private static final Logger log = LoggerFactory.getLogger(SentryClient.class);

    private SentryClientConfig clientConfig;
    private JdbcTemplate sentryJdbcTemplate;

    public SentryClient() {
    }

    public SentryClient(SentryClientConfig config) {
        this.clientConfig = config;
        this.sentryJdbcTemplate = config.getSentryJdbcTemplate();

        try {
            setDriverClass(config.getDriverName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to set Driver Class " + e.getMessage());
        }

    }

    public void setDriverClass(String driverName) throws ClassNotFoundException {
        Class.forName(driverName);
    }

    /**
     * @param roleName : Role name to be created
     */
    public boolean createRole(String roleName) throws SentryClientException {
        this.sentryJdbcTemplate.execute("create role " + roleName);
        log.warn("Sentry role " + roleName + " created successfully.");
        return true;
    }

    /**
     * @param roleName : Role to be deleted.
     */
    public boolean dropRole(String roleName) throws SentryClientException {
        this.sentryJdbcTemplate.execute("drop role " + roleName);
        log.info("Role  " + roleName + " dropped successfully ");
        return true;
    }

    /**
     * @param roleName  : Sentry Role name
     * @param groupName : group name to be granted
     */

    public boolean grantRoleToGroup(String roleName, String groupName) throws SentryClientException {
        String queryString = "GRANT ROLE " + roleName + " TO GROUP " + groupName + "";
        this.sentryJdbcTemplate.execute(queryString);
        log.info("Role " + roleName + " is assigned to group " + groupName + ". ");
        return true;
    }

    /**
     * @param previledge : ALL/Select
     * @param objectType : DATABASE/TABLE
     * @param objectName : Database name or table name
     * @param roleName   : Role name to be granted
     */
    public boolean grantRolePriviledges(String previledge, String objectType, String objectName, String roleName) throws SentryClientException {
        String dbName[] = objectName.split("\\.");
        String useDB = "use " + dbName[0] + "";
        String queryString = "GRANT " + previledge + " ON " + objectType + " " + objectName + "  TO ROLE " + roleName;
        log.info("Sentry Query Formed --" + queryString);
        try {
            this.sentryJdbcTemplate.execute(useDB);
            this.sentryJdbcTemplate.execute(queryString);
            log.info("Successfully assigned priviledge " + previledge + " to role " + roleName + " on " + objectName + ".");
            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Failed to obtain database name from " + objectName + ". Routing to failure.");
            throw new ArrayIndexOutOfBoundsException("Failed to obtain database name from " + objectName + ". Routing to failure." + e.getMessage());
        }
    }

    /**
     * @param roleName : Check role if it is already created by Kylo
     * @return true/false
     */
    public boolean checkIfRoleExists(String roleName) {
        boolean matchFound = false;
        String queryString = "SHOW ROLES";

        List<SentrySearchPolicy> sentryPolicy = this.sentryJdbcTemplate.query(queryString, new SentrySearchPolicyMapper());

        if (sentryPolicy.isEmpty()) {
            matchFound = false;
        } else {
            for (SentrySearchPolicy policyName : sentryPolicy) {
                if (policyName.getRole().equalsIgnoreCase(roleName)) {
                    matchFound = true;
                    break;
                } else {
                    matchFound = false;
                }
            }
        }
        if (matchFound) {
            /**
             * Return true of role is present in result set
             */
            log.info("Role " + roleName + " found in Sentry database.");
            return true;
        } else {
            log.info("Role " + roleName + "  not found in Sentry database.");
            return false;
        }

    }

    public boolean revokeRoleFromGroup() {
        return false;
    }

    public boolean revokeRolePriviledges() {
        return false;
    }

    @SuppressWarnings("static-access")
    public boolean createAcl(String HadoopConfigurationResource, String groups, String allPathForAclCreation, String hdfsPermission) {

        try {
            SentryClientConfig sentryConfig = new SentryClientConfig();
            HDFSUtil hdfsUtil = new HDFSUtil();
            Configuration conf = sentryConfig.getConfig();
            conf = hdfsUtil.getConfigurationFromResources(HadoopConfigurationResource);
            hdfsUtil.splitPathListAndApplyPolicy(allPathForAclCreation, conf, sentryConfig.getFileSystem(), groups, hdfsPermission);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create ACL " + e);

        }
    }

    /**
     * @param HadoopConfigurationResource : Hadoop HDFS Configuraiton Files
     * @param allPathForAclDeletion       : List paths for deleting ACL
     */

    @SuppressWarnings("static-access")
    public boolean flushACL(String HadoopConfigurationResource, String allPathForAclDeletion) {

        try {

            SentryClientConfig sentryConfig = new SentryClientConfig();
            HDFSUtil hdfsUtil = new HDFSUtil();
            Configuration conf = sentryConfig.getConfig();
            conf = hdfsUtil.getConfigurationFromResources(HadoopConfigurationResource);
            hdfsUtil.splitPathListAndFlushPolicy(allPathForAclDeletion, conf, sentryConfig.getFileSystem());
            return true;

        } catch (IOException e) {
            throw new RuntimeException("Unable to flush ACL " + e);

        }

    }


    /**
     * Get default Kylo groups
     */
    public List<HadoopAuthorizationGroup> getAllGroups() {
        List<HadoopAuthorizationGroup> sentryHadoopAuthorizationGroups = new ArrayList<>();
        SentryGroup hadoopAuthorizationGroup = new SentryGroup();

        String sentryGroups = this.clientConfig.getSentryGroups();
        List<String> sentryGroupsList = new ArrayList<String>(Arrays.asList(sentryGroups.split(",")));

        int sentryId = 0;
        String sentryIdPrefix = "kyloGroup";

        for (String group : sentryGroupsList) {
            sentryId = sentryId + 1;
            sentryIdPrefix = sentryIdPrefix + "_" + sentryId;
            hadoopAuthorizationGroup.setId(sentryIdPrefix);
            hadoopAuthorizationGroup.setDescription("This is dummy group generated by Kylo.");
            hadoopAuthorizationGroup.setName(group);
            hadoopAuthorizationGroup.setOwner("thinkbig");
            sentryHadoopAuthorizationGroups.add(hadoopAuthorizationGroup);

            sentryIdPrefix = "kyloGroup";
            hadoopAuthorizationGroup = new SentryGroup();

        }
        return sentryHadoopAuthorizationGroups;
    }

}
