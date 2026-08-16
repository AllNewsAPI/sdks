export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'scope-enum': [
      2,
      'always',
      ['python-sdk', 'javascript-sdk', 'go-sdk', 'java-sdk', 'php-sdk', 'ruby-sdk', 'ci', 'docs', 'deps'],
    ],
  },
};
