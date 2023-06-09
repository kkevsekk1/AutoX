/**
 * Created by Aaron on 7/9/2015.
 */

module.exports = function( grunt ) {

    grunt.loadNpmTasks( 'grunt-babel' );
    grunt.loadNpmTasks( 'grunt-contrib-clean' );
    grunt.loadNpmTasks( 'grunt-banner' );

    var LICENSE = '/****\n * ' +
                  grunt.file.read( './LICENSE', {encoding: 'utf-8'} ).replace( /\n/ig, '\n * ' ) +
                  '\n ****/';

    var loose = {loose: true};

    var base_plugins = [
        ['transform-es2015-arrow-functions', loose],
        ['transform-es2015-block-scoped-functions', loose],
        ['transform-es2015-block-scoping', loose],
        ['transform-es2015-computed-properties', loose],
        ['transform-es2015-constants', loose],
        ['transform-es2015-destructuring', loose],
        ['transform-es2015-for-of', loose],
        ['transform-es2015-function-name', loose],
        ['transform-es2015-literals', loose],
        ['transform-es2015-parameters', loose],
        ['transform-es2015-shorthand-properties', loose],
        ['transform-es2015-spread', loose],
        ['transform-es2015-template-literals', loose],
        ['transform-es2015-classes', loose],
        ['transform-undefined-to-void']
    ];

    var async_plugins = [
        ['transform-async-to-module-method', {module: 'bluebird', method: 'coroutine', loose: true}],
        ['transform-es2015-modules-commonjs', loose],
        ['transform-runtime', loose]
    ];

    grunt.initConfig( {
        babel:     {
            options:   {
                ast:        false,
                sourceMaps: false,
                compact:    false
            },
            build:     {
                options: {
                    plugins: base_plugins.concat( [['transform-es2015-modules-commonjs', loose]] )
                },
                files:   [
                    {
                        expand: true,
                        cwd:    './src/',
                        src:    './**/*.js',
                        dest:   './build/'
                    }
                ]
            },
            tests:     {
                options: {
                    plugins: async_plugins.concat( base_plugins )
                },
                files:   [
                    {
                        expand: true,
                        cwd:    './test/src/',
                        src:    './**/*.js',
                        dest:   './test/build/'
                    }
                ]
            },
            benchmark: {
                options: {
                    plugins: async_plugins.concat( base_plugins )
                },
                files:   [
                    {
                        expand: true,
                        cwd:    './benchmark/src/',
                        src:    './**/*.js',
                        dest:   './benchmark/build/'
                    }
                ]
            }
        },
        usebanner: {
            license: {
                options: {
                    position:  'top',
                    banner:    LICENSE,
                    linebreak: true
                },
                files:   {
                    src: ['./build/**/*.js']
                }
            }
        },
        clean:     {
            build:     {
                src: ['./build']
            },
            tests:     {
                src: ['./test/build']
            },
            benchmark: {
                src: ['./benchmark/build']
            }
        }
    } );

    grunt.registerTask( 'build', ['clean:build', 'babel:build', 'usebanner:license'] );

    grunt.registerTask( 'build-tests', ['clean:tests', 'babel:tests'] );

    grunt.registerTask( 'build-benchmark', ['clean:benchmark', 'babel:benchmark'] );

    grunt.registerTask( 'default', ['build', 'build-benchmark', 'build-tests'] );
};
