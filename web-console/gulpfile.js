var gulp = require('gulp');
var sass = require('gulp-sass');
var webserver = require('gulp-webserver');

var paths = {
    scss: './scss/*.scss'
};

gulp.task('webserver', function() {
  gulp.src('app')
    .pipe(webserver({
        fallback: 'index.html'
    }));
});

gulp.task('styles', function(){
    return gulp.src(paths.scss)
        .pipe(sass().on('error', sass.logError))
        .pipe(gulp.dest('./app/css/'));
});

gulp.task('watch', function() {
    gulp.watch('./scss/**.scss', ['styles']);
});

gulp.task('default', ['webserver','watch','styles']);
