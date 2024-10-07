# Changelog

## 1.0.0 (2024-10-07)


### Features

* add cron parser ([9a1cda0](https://github.com/teletha/belldandy/commit/9a1cda036a99e4fa355e41d0d41cfdc563abe5b7))
* implement ExecutorService API ([c459112](https://github.com/teletha/belldandy/commit/c459112e1a90fcb7da8875d1d76f01e1ead1462a))
* integrate Cron with Scheduler ([53dbfdd](https://github.com/teletha/belldandy/commit/53dbfdd4f80a10c70e7a156511e16e7aff981102))
* reduce memory usage when registering a huge number of tasks ([6580056](https://github.com/teletha/belldandy/commit/65800566bc392543e1efd2c788d888bd7b8f9982))
* remove Cron#next(base, limit) ([769f487](https://github.com/teletha/belldandy/commit/769f48732b326067c57e278de9fe05b2298eb31f))
* remove Part ([f7e6b1c](https://github.com/teletha/belldandy/commit/f7e6b1cdaa0b56d1ab11bf0e086b72b599b678db))
* remove the task-queue thread ([38b9db4](https://github.com/teletha/belldandy/commit/38b9db49e813008d7ae354efb689a53ba3eabf4d))
* reschedule by loop ([2089193](https://github.com/teletha/belldandy/commit/20891931a1cae9dcc95376facc6a2a0b61afec90))
* rewrite cron ([b6084ed](https://github.com/teletha/belldandy/commit/b6084ed217af05e536b817c9f640cfe840b61c65))
* Scheduler extends AbstractExecutorService ([c9cb6bb](https://github.com/teletha/belldandy/commit/c9cb6bbc3effc29ec5750903f4cbed95b51842c9))
* Scheduler manages time as Instant ([7130b4d](https://github.com/teletha/belldandy/commit/7130b4d19bb76777c77ca4f9bf43b552ef190b29))
* support cron scheduler ([2c393bd](https://github.com/teletha/belldandy/commit/2c393bd7ae7f6008a8b6a53783601c4bce81d721))
* Task can hold the execution timing as primitive long ([be51c76](https://github.com/teletha/belldandy/commit/be51c76121dda1ea7b00d23c869e97536048c4aa))
* Task extends FutureTask ([02afd26](https://github.com/teletha/belldandy/commit/02afd2679437fdbd6fc6e7b02f45c2e321f7e39c))


### Bug Fixes

* Field and Type are top-level class now ([1bc84fe](https://github.com/teletha/belldandy/commit/1bc84fedb3fbcba8984c89cbafd993dc0327dd87))
* more testable ([75aca96](https://github.com/teletha/belldandy/commit/75aca968a69ff6179c4337f94b99fe8990558725))
* optimize #awaitTermination ([2dceb73](https://github.com/teletha/belldandy/commit/2dceb735bffbdf225df0bd590ebf9437cdfcc903))
* optimize Field ([3999a2d](https://github.com/teletha/belldandy/commit/3999a2d2c82fa93f2f5d572c1fef922835636a24))
* optimize schduling ([e81262e](https://github.com/teletha/belldandy/commit/e81262e85caa994ad64fc2264f8ef596faca2133))
* optimize Type ([0656f5b](https://github.com/teletha/belldandy/commit/0656f5b7fb97d6413d83917d8c1b71a8aacb230b))
* remove loop for reschedule ([4021785](https://github.com/teletha/belldandy/commit/4021785df022b9c24d2385cfbbaf9f9c35df21ae))
* remove unused code ([bd315bd](https://github.com/teletha/belldandy/commit/bd315bd1dce4c9f74f78de72d758ced38f78ae00))
* remove unused code ([71628df](https://github.com/teletha/belldandy/commit/71628dfbac5019cd87f843005129ddbb13f67920))
* rename to Scheduler ([371db25](https://github.com/teletha/belldandy/commit/371db25d219232386df4d947d2b99d7c9994aee8))
* support inheritable thread local. ([2a5eca3](https://github.com/teletha/belldandy/commit/2a5eca3d3b4d51491e0840d67d0e2bf7d3ccb8e2))
* test ([977e435](https://github.com/teletha/belldandy/commit/977e4356a6331f463c5cc6d3877f5e79e03ab121))
* use the specialized-primitive function ([734d8af](https://github.com/teletha/belldandy/commit/734d8af00f0eea59a1c72af151e520a3280aed51))
