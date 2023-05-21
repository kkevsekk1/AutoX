(function() {
    const browser = require('./browser-libs/index.js')
    const utils = require("./utils/index.js");
    const axios = require("./axios.min.js");
    axios.defaults.transformRequest = [];
    axios.browser = browser;
    axios.utils = utils;
    
    module.exports = axios;
})()