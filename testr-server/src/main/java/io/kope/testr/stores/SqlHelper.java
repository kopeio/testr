package io.kope.testr.stores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class SqlHelper {

	private static final Logger log = LoggerFactory.getLogger(SqlHelper.class);

	final DataSource dataSource;

	public SqlHelper(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public SqlCommand command(String sql, Object... args) {
		return new SqlCommand(sql, args);
	}

	public int executeUpdate(String sql, Object... args) {
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				for (int i = 0; i < args.length; i++) {
					Object arg = args[i];
					ps.setObject(i + 1, arg);
				}

				log.info("Executing SQL update {}", sql);

				return ps.executeUpdate();
			}
		} catch (SQLException e) {
			throw new StoreException("Error inserting row into data store", e);
		}
	}

	class SqlCommand {
		final String sql;
		final Object[] args;

		public SqlCommand(String sql, Object[] args) {
			this.sql = sql;
			this.args = args;
		}

		public <T> List<T> map(Function<ResultSet, T> mapper) {
			List<T> results = Lists.newArrayList();
			try (Connection connection = dataSource.getConnection()) {
				try (PreparedStatement ps = connection.prepareStatement(sql)) {
					for (int i = 0; i < args.length; i++) {
						Object arg = args[i];
						ps.setObject(i + 1, arg);
					}

					log.info("Executing SQL query {}", sql);
					try (ResultSet rs = ps.executeQuery()) {
						while (rs.next()) {
							T t = mapper.apply(rs);
							results.add(t);
						}
					}
				}
			} catch (SQLException e) {
				throw new StoreException("Error querying data store", e);
			}
			return results;
		}

		public <T> Optional<T> fetchOne() {
			List<T> results = map((ResultSet rs) -> {
				try {
					return (T) rs.getObject(1);
				} catch (SQLException e) {
					throw new StoreException("Error reading row", e);
				}
			});
			if (results.size() == 1) {
				return Optional.of(results.get(0));
			}
			if (results.size() == 0) {
				return Optional.empty();
			}
			throw new StoreException("Found multiple rows when expecting one or zero");
		}

	}
}
