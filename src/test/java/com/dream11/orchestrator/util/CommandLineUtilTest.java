package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class CommandLineUtilTest {

  @Test
  void testExecuteStdOut() {
    // Act
    CommandLineUtil.CommandResult result = CommandLineUtil.execute("echo", "hello");
    // Assert
    assertThat(result.getExitCode()).isZero();
    assertThat(result.getStdOut()).isEqualTo("hello");
    assertThat(result.getStdErr()).isEmpty();
  }

  @Test
  void testExecuteStdErr() {
    // Act
    CommandLineUtil.CommandResult result = CommandLineUtil.execute("mv");

    // Assert
    assertThat(result.getExitCode()).isNotZero();
    assertThat(result.getStdOut()).isEmpty();
    assertThat(result.getStdErr()).contains("usage: mv [-f | -i | -n] [-hv] source target");
  }

  @Test
  void testExecuteInvalidCommand() {
    // Act & Assert
    assertThatThrownBy(() -> CommandLineUtil.execute("invalid"))
        .isInstanceOf(IOException.class)
        .hasMessage("Cannot run program \"invalid\": error=2, No such file or directory");
  }
}
