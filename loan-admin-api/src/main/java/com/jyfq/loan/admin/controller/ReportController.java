package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.ChannelReportQueryDTO;
import com.jyfq.loan.model.dto.HourlyReportQueryDTO;
import com.jyfq.loan.model.dto.InstitutionReportQueryDTO;
import com.jyfq.loan.model.vo.ChannelReportSummaryVO;
import com.jyfq.loan.model.vo.ChannelReportVO;
import com.jyfq.loan.model.vo.HourlyReportVO;
import com.jyfq.loan.model.vo.InstitutionReportRowVO;
import com.jyfq.loan.model.vo.InstitutionReportSummaryVO;
import com.jyfq.loan.service.AdminInstitutionReportService;
import com.jyfq.loan.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Report statistic APIs.
 */
@Tag(name = "Report statistics")
@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportController {

    private final AdminReportService adminReportService;
    private final AdminInstitutionReportService adminInstitutionReportService;

    @Operation(summary = "Hourly report")
    @GetMapping("/hourly")
    public R<HourlyReportVO> hourly(HourlyReportQueryDTO query) {
        return R.ok(adminReportService.hourlyReport(query));
    }

    @Operation(summary = "Channel report")
    @GetMapping("/channel")
    public R<ChannelReportVO> channel(ChannelReportQueryDTO query) {
        return R.ok(adminReportService.channelReport(query));
    }

    @Operation(summary = "Channel summary report")
    @GetMapping("/channel-summary")
    public R<ChannelReportSummaryVO> channelSummary(ChannelReportQueryDTO query) {
        return R.ok(adminReportService.channelSummary(query));
    }

    @Operation(summary = "Institution report summary")
    @GetMapping("/institution/summary")
    public R<InstitutionReportSummaryVO> institutionSummary(InstitutionReportQueryDTO query) {
        return R.ok(adminInstitutionReportService.summary(query));
    }

    @Operation(summary = "Institution report list")
    @GetMapping("/institution/list")
    public R<PageResult<InstitutionReportRowVO>> institutionList(InstitutionReportQueryDTO query) {
        return R.ok(adminInstitutionReportService.page(query));
    }

    @Operation(summary = "Institution report export")
    @GetMapping("/institution/export")
    public void institutionExport(InstitutionReportQueryDTO query, HttpServletResponse response) throws IOException {
        List<InstitutionReportRowVO> rows = adminInstitutionReportService.exportRows(query);
        String fileName = URLEncoder.encode("institution-report.csv", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("\u673a\u6784\u540d\u79f0,\u673a\u6784ID,\u673a\u6784\u7f16\u7801,\u603b\u8ba2\u5355,\u6210\u529f\u8ba2\u5355,\u6210\u529f\u7387(%),\u603b\u91d1\u989d,\u6263\u8d39\u91d1\u989d\n");
        for (InstitutionReportRowVO row : rows) {
            builder.append(csv(row.getInstName())).append(',')
                    .append(value(row.getInstId())).append(',')
                    .append(csv(row.getInstCode())).append(',')
                    .append(value(row.getTotalOrders())).append(',')
                    .append(value(row.getSuccessOrders())).append(',')
                    .append(value(row.getSuccessRate())).append(',')
                    .append(value(row.getTotalAmount())).append(',')
                    .append(value(row.getDeductAmount())).append('\n');
        }
        response.getWriter().write(builder.toString());
    }

    private String value(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
