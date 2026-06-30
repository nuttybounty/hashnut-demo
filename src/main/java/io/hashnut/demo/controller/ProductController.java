package io.hashnut.demo.controller;

import io.hashnut.demo.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final JdbcTemplate jdbc;

    public ProductController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/products")
    public Map<String, Object> listProducts() {
        List<Product> products = jdbc.query(
                "SELECT id, name, description, price, image_url, created_at FROM products ORDER BY id",
                (rs, rowNum) -> {
                    Product p = new Product();
                    p.setId(rs.getInt("id"));
                    p.setName(rs.getString("name"));
                    p.setDescription(rs.getString("description"));
                    p.setPrice(rs.getBigDecimal("price").stripTrailingZeros().toPlainString());
                    p.setImageUrl(rs.getString("image_url"));
                    p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return p;
                });
        return Collections.singletonMap("data", products);
    }

    /**
     * 返回支持的链+币种列表（从 t_coin_info 读取，只返回有对应 splitter 配置的）
     */
    @GetMapping("/chains")
    public Map<String, Object> listSupportedChains() {
        List<Map<String, Object>> result = jdbc.queryForList(
                "SELECT c.chain_code, c.coin_code, c.chain_label, c.coin_label, c.contract_address, c.decimals " +
                "FROM t_coin_info c " +
                "INNER JOIN t_hashnut_api_key k ON c.chain_code = k.chain_code " +
                "ORDER BY c.chain_code, c.coin_code");
        return Collections.singletonMap("data", result);
    }
}
