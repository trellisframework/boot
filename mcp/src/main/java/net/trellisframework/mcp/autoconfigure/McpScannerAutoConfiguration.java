package net.trellisframework.mcp.autoconfigure;

import io.modelcontextprotocol.server.McpServerFeatures;
import net.trellisframework.mcp.scanner.McpComponentScanner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnClass(McpServerFeatures.class)
@Import(McpComponentScanner.class)
public class McpScannerAutoConfiguration {
}
