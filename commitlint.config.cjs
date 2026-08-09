// コミットメッセージが .gitmessage の規約「[#XXXXX] type: 概要」に沿っているかを検証する。
// 標準の @commitlint/config-conventional は type(scope): subject 形式を前提としており、
// 本リポジトリのチケット番号プレフィックス付き日本語件名とは噛み合わないため継承しない。
const HEADER_PATTERN =
  /^\[#\d{5}\] (feat|fix|docs|style|refactor|test|chore|perf|build|ci|revert): .+$/u;

module.exports = {
  plugins: [
    {
      rules: {
        "ticket-header-format": (parsed) => {
          const header = parsed.header || "";
          return [
            HEADER_PATTERN.test(header),
            'コミットメッセージは "[#XXXXX] type: 概要" 形式にしてください（例: [#00001] feat: リポジトリ新規構築）',
          ];
        },
      },
    },
  ],
  rules: {
    "ticket-header-format": [2, "always"],
    "header-max-length": [2, "always", 100],
  },
};
