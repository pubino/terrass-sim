export default [{
  files: ["web/*.js"],
  rules: {
    "eol-last": ["error", "always"],
    "eqeqeq": "error",
    "indent": ["error", 2],
    "quotes": ["error", "double", { "allowTemplateLiterals": true }],
    "semi": ["error", "always"],
    "no-unreachable": "error",
    "no-unused-vars": "error",
    "no-unsafe-finally": "error",
    "prefer-const": "error",
  },
}];
