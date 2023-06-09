/**
 * Created by Aaron on 7/9/2015.
 */

var Promise     = require( 'bluebird' ),
    bluebird_co = require( './build/yield_handler.js' );

Promise.coroutine.addYieldHandler( bluebird_co.toPromise );

module.exports = bluebird_co;
