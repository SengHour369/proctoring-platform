package com.example.attemptservice.excel.entity;

import com.example.attemptservice.common.entity.BaseEntity;
import com.example.attemptservice.excel.enums.ExcelSheetOperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A structural change to a sheet within an {@link ExcelSession} — insert, delete, rename, hide,
 * protect (25.4.5, 25.7.3) — kept separate from {@link ExcelCellEdit} because it isn't a value
 * change on any one cell.
 */
@Entity
@Table(
        name = "excel_sheet_operations",
        indexes = @Index(name = "ix_excel_sheet_operations_session_time", columnList = "excel_session_id, occurred_at"))
@Getter
@Setter
public class ExcelSheetOperation extends BaseEntity {

    @Column(name = "excel_session_id", nullable = false)
    private Long excelSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 16)
    private ExcelSheetOperationType operationType;

    @Column(name = "sheet_name", nullable = false, length = 100)
    private String sheetName;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    /** Old name for a RENAME, or other operation-specific detail. */
    @Column(length = 500)
    private String detail;
}
