/**
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 18/02/16
 */
package it.neo4j.jdbc.bolt.data;

import org.junit.BeforeClass;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.internal.InternalStatementResult;
import org.neo4j.driver.internal.ParameterSupport;
import org.neo4j.driver.internal.spi.Connection;
import org.neo4j.driver.internal.spi.StreamCollector;
import org.neo4j.driver.internal.value.FloatValue;
import org.neo4j.driver.internal.value.IntegerValue;
import org.neo4j.driver.internal.value.StringValue;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.neo4j.driver.v1.Values.values;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class ResultSetData {
	public static List<Object[]> RECORD_LIST_EMPTY = Collections.emptyList();
	public static List<Object[]> RECORD_LIST_ONE_ELEMENT;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS_DIFF;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS_MIXED;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS_NODES;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS_RELATIONS;

	public static String[] KEYS_RECORD_LIST_EMPTY                   = new String[] {};
	public static String[] KEYS_RECORD_LIST_ONE_ELEMENT             = new String[] { "columnA", "columnB" };
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS           = KEYS_RECORD_LIST_ONE_ELEMENT;
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS_DIFF      = new String[] { "columnA", "columnB", "columnC" };
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED     = new String[] { "columnInt", "columnString", "columnFloat", "columnShort",
			"columnDouble" };
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS_NODES     = new String[] { "node" };
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS_RELATIONS = new String[] { "relation" };

	private static Method runResponseCollectorMethod;
	private static Method pullAllResponseCollectorMethod;

	@BeforeClass public static void initialize() {
		RECORD_LIST_ONE_ELEMENT = new LinkedList<>();
		RECORD_LIST_ONE_ELEMENT.add(new Object[] { "valueA1", "valueB1" });

		RECORD_LIST_MORE_ELEMENTS = new LinkedList<>();
		RECORD_LIST_MORE_ELEMENTS.add(new Object[] { "valueA1", "valueB1" });
		RECORD_LIST_MORE_ELEMENTS.add(new Object[] { "valueA2", "valueB2" });
		RECORD_LIST_MORE_ELEMENTS.add(new Object[] { "valueA3", "valueB3" });

		RECORD_LIST_MORE_ELEMENTS_MIXED = new LinkedList<>();

		RECORD_LIST_MORE_ELEMENTS_MIXED.add(new Object[] { 1, "value1", 0.1f, (short) 1, 02.29D });
		RECORD_LIST_MORE_ELEMENTS_MIXED.add(new Object[] { 2, "value2", 0.2f, (short) 2, 20.16D });

		RECORD_LIST_MORE_ELEMENTS_NODES = new LinkedList<>();

		RECORD_LIST_MORE_ELEMENTS_NODES.add(new Object[] { new InternalNode(1, new LinkedList<String>() {
			{
				this.add("label1");
				this.add("label2");
			}
		}, new HashMap<String, Value>() {
			{
				this.put("property1", new StringValue("value1"));
				this.put("property2", new IntegerValue(1));
			}
		}) });

		RECORD_LIST_MORE_ELEMENTS_NODES.add(new Object[] { new InternalNode(2, new LinkedList<String>() {
			{
				this.add("label");
			}
		}, new HashMap<String, Value>() {
			{
				this.put("property", new FloatValue(1.6f));
			}
		}) });

		RECORD_LIST_MORE_ELEMENTS_RELATIONS = new LinkedList<>();

		RECORD_LIST_MORE_ELEMENTS_RELATIONS.add(new Object[] { new InternalRelationship(1, 0, 0, "type1", new HashMap<String, Value>() {
			{
				this.put("property1", new StringValue("value"));
				this.put("property2", new IntegerValue(100));
			}
		}) });

		RECORD_LIST_MORE_ELEMENTS_RELATIONS.add(new Object[] { new InternalRelationship(2, 0, 0, "type2", new HashMap<String, Value>() {
			{
				this.put("property", new FloatValue(2.6f));
			}
		}) });

		RECORD_LIST_MORE_ELEMENTS_DIFF = new LinkedList<>();
		RECORD_LIST_MORE_ELEMENTS_DIFF.add(new Object[] { "valueA", "valueB" });
		RECORD_LIST_MORE_ELEMENTS_DIFF.add(new Object[] { "valueA", "valueB", "valueC" });

		fixPublicForInternalResultCursor();
	}

	/**
	 * open up some package scope method for public usage
	 */
	private static void fixPublicForInternalResultCursor() {
		try {
			runResponseCollectorMethod = InternalStatementResult.class.getDeclaredMethod("runResponseCollector");
			runResponseCollectorMethod.setAccessible(true);
			pullAllResponseCollectorMethod = InternalStatementResult.class.getDeclaredMethod("pullAllResponseCollector");
			pullAllResponseCollectorMethod.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * hackish way to get a {@link InternalStatementResult}
	 *
	 * @param keys
	 * @param data
	 * @return
	 */
	public static StatementResult buildResultCursor(String[] keys, List<Object[]> data) {

		try {
			Connection connection = mock(Connection.class);

			InternalStatementResult cursor = new InternalStatementResult(connection, null);
			StreamCollector responseCollector = (StreamCollector) runResponseCollectorMethod.invoke(cursor);
			responseCollector.keys(keys);
			responseCollector.done();

			StreamCollector pullAllResponseCollector = (StreamCollector) pullAllResponseCollectorMethod.invoke(cursor);

			data.forEach(vals -> pullAllResponseCollector.record(values(vals)));
			pullAllResponseCollector.done();
			connection.run("<unknown>", ParameterSupport.NO_PARAMETERS, responseCollector);
			connection.pullAll(pullAllResponseCollector);
			//connection.sendAll();

			return cursor;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}