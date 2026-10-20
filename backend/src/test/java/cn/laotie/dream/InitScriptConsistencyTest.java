package cn.laotie.dream;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 部署用的 sql/init-mysql.sql 与启动时执行的 schema.sql 建表语句必须一致，防止改表时只改一处。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
public class InitScriptConsistencyTest {
    /** 去掉注释、引擎与字符集子句并压缩空白后，按语句拆分出全部建表语句。 */
    private List<String> tables(String path) throws Exception {
        String text=new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);
        StringBuilder sql=new StringBuilder();
        for(String line:text.split("\\r?\\n")) if(!line.trim().startsWith("--")) sql.append(line).append('\n');
        List<String> result=new ArrayList<>();
        for(String statement:sql.toString().split(";")) {
            String normalized=statement.replaceAll("\\)\\s*ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci\\s*$",")").replaceAll("\\s+"," ").trim();
            if(normalized.startsWith("CREATE TABLE")) result.add(normalized);
        }
        return result;
    }
    @Test void initScriptMatchesSchema() throws Exception {
        List<String> schema=tables("src/main/resources/schema.sql"), init=tables("sql/init-mysql.sql");
        assertThat(schema).hasSize(25);
        assertThat(init).containsExactlyElementsOf(schema);
        String header=new String(Files.readAllBytes(Paths.get("sql/init-mysql.sql")),StandardCharsets.UTF_8);
        assertThat(header).contains("CREATE DATABASE IF NOT EXISTS dream_realize DEFAULT CHARACTER SET utf8mb4","USE dream_realize;");
    }
}
