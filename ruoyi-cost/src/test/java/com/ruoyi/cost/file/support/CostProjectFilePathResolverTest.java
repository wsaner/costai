package com.ruoyi.cost.file.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;

class CostProjectFilePathResolverTest
{
    @TempDir
    Path tempDir;

    private String previousProfile;
    private final CostProjectFilePathResolver resolver = new CostProjectFilePathResolver();

    @BeforeEach
    void setUp()
    {
        previousProfile = RuoYiConfig.getProfile();
        new RuoYiConfig().setProfile(tempDir.toString());
    }

    @AfterEach
    void tearDown()
    {
        new RuoYiConfig().setProfile(previousProfile);
    }

    @Test
    void resolvesOnlyPrivateProjectFiles()
    {
        Path expected = tempDir.resolve("private/project/2026/09/01/a.txt").toAbsolutePath().normalize();
        assertEquals(expected, resolver.resolve("/profile/private/project/2026/09/01/a.txt"));
        assertThrows(ServiceException.class,
                () -> resolver.resolve("/profile/private/project/../../avatar/user.png"));
        assertThrows(ServiceException.class,
                () -> resolver.resolve("/profile/upload/public.txt"));
    }
}
