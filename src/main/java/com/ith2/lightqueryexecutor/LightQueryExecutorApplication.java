package com.ith2.lightqueryexecutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
public class LightQueryExecutorApplication {

	private final JdbcTemplate jdbcTemplate;

	public LightQueryExecutorApplication(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public static void main(String[] args) {
		SpringApplication.run(LightQueryExecutorApplication.class, args);
	}

	private static List<Map<String, Object>> upper(List<Map<String, Object>> list) {

		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> item : list) {
			Map<String, Object> upperCaseMap = new HashMap<>();
			for (Map.Entry<String, Object> entry : item.entrySet()) {
				String upperCaseKey = entry.getKey().toUpperCase(); // 将键名转换为大写
				upperCaseMap.put(upperCaseKey, entry.getValue()); // 添加到新 Map
			}
			result.add(upperCaseMap);
		}
		return result;
	}

	@GetMapping("/api/query")
	public Map<String, Object> query(@RequestParam(required = false) String sql) {
		Map<String, Object> result = new HashMap<>();

		if (sql == null || sql.isEmpty()) {
			result.put("error", "SQL query cannot be null or empty.");
			return result;
		}

		if (!sql.toLowerCase().contains("limit")) {
			sql += " LIMIT 50";
		}

		try {
			List<Map<String, Object>> list = upper(jdbcTemplate.queryForList(sql));
			list.stream().map(row -> {
				if (row != null) {
					return Collections.emptyMap(); // 如果行为空，返回空 Map
				}
				return row.entrySet().stream()
						.collect(Collectors.toMap(
								entry -> entry.getKey() != null ? entry.getKey().toUpperCase() : null, // 将键转换为大写，检查空值
								Map.Entry::getValue,
								(existing, replacement) -> existing // 处理重复键的情况
						));
			});

			result.put("rows", list);
			result.put("total", list.size());
			result.put("rowsTotal", list.size());
			result.put("columns", getColumnNames(sql));
		} catch (Exception e) {
			e.printStackTrace();
			result.put("error", "Error executing query: " + e.getMessage());
		}

		return result;
	}

	private List<Field> getColumnNames(String sql) {
		List<Field> fieldNames = new ArrayList<>();
		try (Connection conn = jdbcTemplate.getDataSource().getConnection();
		     Statement stmt = conn.createStatement();
		     ResultSet rs = stmt.executeQuery(sql)) {

			int columnCount = rs.getMetaData().getColumnCount();
			for (int i = 1; i <= columnCount; i++) {
				String columnName = rs.getMetaData().getColumnName(i);
				fieldNames.add(new Field(columnName != null ? columnName.toUpperCase() : columnName));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return fieldNames;
	}

	static class Field {
		String fieldName;

		public Field(String fieldName) {
			this.fieldName = fieldName;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public String toString() {
			return "Field{" +
					"fieldName='" + fieldName + '\'' +
					'}';
		}
	}
}
