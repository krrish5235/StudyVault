package com.studyvault.admin;

import com.studyvault.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dashboard metrics computed with read-only aggregate SQL against the live
 * database - no fabricated numbers anywhere.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final JdbcTemplate jdbc;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        Long revenueTotalMinor = jdbc.queryForObject(
                "select coalesce(sum(amount_minor), 0) from payments where status = 'CAPTURED'", Long.class);
        Long revenue30dMinor = jdbc.queryForObject(
                "select coalesce(sum(amount_minor), 0) from payments where status = 'CAPTURED' and created_at > now() - interval '30 days'",
                Long.class);
        Long ordersTotal = jdbc.queryForObject("select count(*) from orders", Long.class);
        Long ordersPaid = jdbc.queryForObject("select count(*) from orders where status = 'PAID'", Long.class);
        Long orders30d = jdbc.queryForObject(
                "select count(*) from orders where created_at > now() - interval '30 days'", Long.class);
        Long usersTotal = jdbc.queryForObject("select count(*) from users", Long.class);
        Long users30d = jdbc.queryForObject(
                "select count(*) from users where created_at > now() - interval '30 days'", Long.class);
        Long productsPublished = jdbc.queryForObject(
                "select count(*) from products where status = 'PUBLISHED'", Long.class);
        Long downloads30d = jdbc.queryForObject(
                "select count(*) from downloads where created_at > now() - interval '30 days'", Long.class);
        Long reviewsPending = jdbc.queryForObject("select count(*) from reviews where status = 'PUBLISHED'", Long.class);

        BigDecimal aovMinor = ordersPaid != null && ordersPaid > 0
                ? BigDecimal.valueOf(revenueTotalMinor == null ? 0 : revenueTotalMinor)
                        .divide(BigDecimal.valueOf(ordersPaid), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        double conversion = ordersTotal != null && ordersTotal > 0
                ? Math.round(1000.0 * ordersPaid / ordersTotal) / 10.0
                : 0.0;

        List<Map<String, Object>> revenueByDay = jdbc.queryForList(
                "select d::date as day, coalesce(sum(p.amount_minor) filter (where p.status = 'CAPTURED'), 0) as revenue_minor, "
                        + "count(p.id) filter (where p.status = 'CAPTURED') as paid_orders "
                        + "from generate_series(current_date - interval '13 days', current_date, interval '1 day') d "
                        + "left join payments p on p.created_at::date = d::date "
                        + "group by d::date order by d::date");

        List<Map<String, Object>> topProducts = jdbc.queryForList(
                "select p.title, p.slug, count(oi.id) as units_sold, coalesce(sum(oi.line_total_minor), 0) as revenue_minor "
                        + "from order_items oi join orders o on o.id = oi.order_id and o.status = 'PAID' "
                        + "join products p on p.id = oi.product_id "
                        + "group by p.title, p.slug order by units_sold desc limit 5");

        Map<String, Object> metrics = new java.util.LinkedHashMap<>();
        metrics.put("revenueTotalMinor", revenueTotalMinor == null ? 0 : revenueTotalMinor);
        metrics.put("revenue30dMinor", revenue30dMinor == null ? 0 : revenue30dMinor);
        metrics.put("ordersTotal", ordersTotal);
        metrics.put("ordersPaid", ordersPaid);
        metrics.put("orders30d", orders30d);
        metrics.put("usersTotal", usersTotal);
        metrics.put("users30d", users30d);
        metrics.put("productsPublished", productsPublished);
        metrics.put("downloads30d", downloads30d);
        metrics.put("averageOrderValueMinor", aovMinor);
        metrics.put("conversionRatePercent", conversion);
        metrics.put("revenueByDay", revenueByDay);
        metrics.put("topProducts", topProducts);
        return ApiResponse.ok(metrics);
    }
}
