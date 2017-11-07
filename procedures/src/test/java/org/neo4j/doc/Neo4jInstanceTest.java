/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.doc;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;

import static org.junit.Assert.*;

public class Neo4jInstanceTest {

    private final String query = "CALL dbms.procedures()";

    @Test
    public void newCommunityInstance() throws Exception {
//        GraphDatabaseService db = new Neo4jInstance().newCommunityInstance();
        GraphDatabaseService db = new Neo4jInstance().newEnterpriseInstance();
        Result result = db.execute(query);
        System.out.println("---");
        System.out.println(result.resultAsString());
        System.out.println("---");
        result.close();
        db.shutdown();
    }

}
