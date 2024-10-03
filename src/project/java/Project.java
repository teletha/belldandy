
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

public class Project extends bee.api.Project {
    {
        product("com.github.teletha", "belldandy", "0.1");
        license(MIT);

        require("com.github.teletha", "sinobu").atTest();
        require("com.github.teletha", "antibug").atTest();

        versionControlSystem("https://github.com/teletha/belldandy");
    }
}