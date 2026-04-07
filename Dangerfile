danger.import_dangerfile(github: 'RevenueCat/Dangerfile')

# Prevent importing pre-built material icons in :ui:revenuecatui.
# The module avoids the material-icons runtime dependency by defining custom
# inline icon vectors using materialIcon/materialPath. Using Icons.Filled.* etc.
# causes NoClassDefFoundError at runtime in apps that don't bundle the artifact.
revenuecatui_kt = (git.modified_files + git.added_files)
  .select { |f| f.start_with?("ui/revenuecatui/") && f.end_with?(".kt") }

revenuecatui_kt.each do |file|
  diff = git.diff_for_file(file)
  next unless diff

  diff.patch.each_line do |line|
    next unless line.start_with?("+") && !line.start_with?("+++")
    if line.match?(/^\+\s*import androidx\.compose\.material\.icons\./) &&
       !line.match?(/^\+\s*import androidx\.compose\.material\.icons\.material(Icon|Path)/)
      warn(
        "`#{file}` adds a direct material-icons import (`#{line.sub(/^\+/, '').strip}`). " \
        "`:ui:revenuecatui` avoids the `material-icons` runtime dependency — " \
        "add a custom inline icon to `icons/` instead (see `CircleOutlined.kt`).",
        file: file,
      )
    end
  end
end
