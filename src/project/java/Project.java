
/*
 * Copyright (C) 2024 The BELLDANDY Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
import static bee.api.License.*;

import javax.lang.model.SourceVersion;

public class Project extends bee.api.Project {
    {
        product("com.github.teletha", "belldandy", ref("version.txt"));
        license(MIT);
        describe("""
                Belldandy provides various APIs specialized for date/time related operations using Date-Time API and virtual threads.

                - VIrtual thread based Scheduler
                - Cron Expression
                """);

        require(SourceVersion.RELEASE_21);
        require("com.github.teletha", "sinobu");
        require("com.github.teletha", "antibug").atTest();

        versionControlSystem("https://github.com/teletha/belldandy");
    }
}