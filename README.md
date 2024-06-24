<img src="src/main/resources/META-INF/pluginIcon.svg" width="80" height="80" alt="icon" align="left"/>

HotSpot Crash Examiner
===

[![Official JetBrains Project][jb-official-svg]][jb-official]
[![Contributions welcome][contributions-welcome-svg]][contributions-welcome]
![Build](https://github.com/JetBrains/HotSpotCrashExaminerPlugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/24675-hotspot-crash-examiner.svg)](https://plugins.jetbrains.com/plugin/24675-hotspot-crash-examiner)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/24675-hotspot-crash-examiner.svg)](https://plugins.jetbrains.com/plugin/24675-hotspot-crash-examiner)

<!-- TOC -->
* [HotSpot Crash Examiner](#hotspot-crash-examiner)
  * [Overview](#overview)
    * [Toolwindow](#toolwindow)
    * [Address Hints](#address-hints)
    * [Find Address](#find-address)
    * [Hints](#hints)
    * [Go To Declaration](#go-to-declaration)
    * [Configuration Options](#configuration-options)
  * [Installation](#installation)
  * [Resources](#resources)
<!-- TOC -->

<!-- Plugin description -->
Provides IDE capabilities
to examining [HotSpot JVM fatal error logs](https://docs.oracle.com/javase/10/troubleshoot/fatal-error-log.htm):
* Syntax highlighting
* Structured view
* Folding
* Go to declaration for Java names
* A dedicated tool window
* Documentation hints for addresses, keywords, sizes, etc.

Additional features:
* Configurable auto-folding of sections (`Settings | Other Settings | HotSpot Crash Examiner`).
* The tool window lists important properties from the log, their explanation,
  and detailed analysis.
* Highlights the relevant portion of the log when clicking on a tool window element.
* Mark the properties that require attention such as low physical memory on the JVM host.
  Limits are configurable in the Settings dialog.
* Go to declaration for Java classes and methods.
* `Find Address` action from the editor's context menu will highlight all occurrences of the currently selected address
  and those near it (configurable in the Settings dialog).
  If the address belongs to some thread's stack or can be found in the memory map, that will also be highlighted.
* Documentation hints for register names, signals, and other keywords; detailed info about addresses.

Automatically recognizes the files matching these patterns: `hs_err_*.log`, `java_error_in_*.log`, and `crash*.txt`.

To view any file with this plugin, select the file in the project view, choose
`Override File Type` and then `HotSpot Fatal Error Log`
<!-- Plugin description end -->

## Overview

### Toolwindow
Configurable warnings in the tool window draw attention to unusual features of the crash:
![Toolwindow warning](https://private-user-images.githubusercontent.com/28651297/341443362-c49974be-db55-4994-82d6-2d9476dbc444.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MTg4OTM3NzgsIm5iZiI6MTcxODg5MzQ3OCwicGF0aCI6Ii8yODY1MTI5Ny8zNDE0NDMzNjItYzQ5OTc0YmUtZGI1NS00OTk0LTgyZDYtMmQ5NDc2ZGJjNDQ0LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDA2MjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwNjIwVDE0MjQzOFomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTVhYmRjYTFhNDVmYWRhZjA3MWQyMTUzNWMwMjBmMTAxOGZhZTdlYzQyYTcwN2UwY2IwZDg3Y2MxOWJhZDQzMWUmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.SzpsD8dqMTcH2yid-nuiSyMWiQxIGzuEBzJSMYL3RD8)

A human-readable analysis of the crash:
![Analysis](https://private-user-images.githubusercontent.com/28651297/341443372-e6a94fb6-043f-4e33-ab8a-9f04536a038f.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MTg4OTM3NzgsIm5iZiI6MTcxODg5MzQ3OCwicGF0aCI6Ii8yODY1MTI5Ny8zNDE0NDMzNzItZTZhOTRmYjYtMDQzZi00ZTMzLWFiOGEtOWYwNDUzNmEwMzhmLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDA2MjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwNjIwVDE0MjQzOFomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWY3ZjMyOTMzYWVkZjhhNjFiZDU1ZGFkNTVlMjU1OWE3NDFjZDc0MmNiOWY5NGI4ZGIwNGU3Mzk4N2Y2MTlhMjEmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.MtOOj0VX2LFjgZN3yCZ80QQbGJbydr_S3wsLX3epQHw)

### Address Hints
Hold the mouse pointer over an address to try to resolve it.
Works for thread, stack, Java heap, dynamic libraries, and other addresses:
![Heap address resolved](https://private-user-images.githubusercontent.com/28651297/341443348-e34e9eeb-60e4-4e29-8453-7ed8973f1b94.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MTg4OTM3NzgsIm5iZiI6MTcxODg5MzQ3OCwicGF0aCI6Ii8yODY1MTI5Ny8zNDE0NDMzNDgtZTM0ZTllZWItNjBlNC00ZTI5LTg0NTMtN2VkODk3M2YxYjk0LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDA2MjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwNjIwVDE0MjQzOFomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWNkNzEzZDgzMGY5YzE1YmQ2OTFlZjIzZDVkNGMxZjAyNWY5MzA3NjdhMmM3NzIwNjA2ZDI3ZWZjMDRhNGMxOWYmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.k0haJ_E-7EfzY3eo6oLcjVt3W-fF4geYSqRkyLcgR3Q)

### Find Address
Pick the `Find Address` option from the context menu 
to select the address under the cursor and addresses near it everywhere in the log file:
![Resolves an address to the JavaThread name](https://private-user-images.githubusercontent.com/28651297/341443328-ea84dd01-ae8a-4d5b-a293-df7dfd7fbe23.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MTg4OTI3OTIsIm5iZiI6MTcxODg5MjQ5MiwicGF0aCI6Ii8yODY1MTI5Ny8zNDE0NDMzMjgtZWE4NGRkMDEtYWU4YS00ZDViLWEyOTMtZGY3ZGZkN2ZiZTIzLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDA2MjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwNjIwVDE0MDgxMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTYzM2UyNTU3NjFkOGJlMjhiNjM5MTc1YTYwOGRhZWJhYjc0NmQ5MWQ2Mjc1OWY1ZmY4ZGJiOTk4NmM0YmNiOWUmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.UTrbXbPROJRMVoJJLFZQuYEUsnLD_N_JsmdWkhbj4Oo)
Addresses do not have to match textually or even appear in the log.
For example, address pointing into a memory-mapped region will highlight that
region in the log.

### Hints
Numbers that denote the size are converted into a more human-readable form: 
![Size hint](https://private-user-images.githubusercontent.com/28651297/341443353-94527094-b8fc-4832-902a-0df27fa50666.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MTg4OTM3NzgsIm5iZiI6MTcxODg5MzQ3OCwicGF0aCI6Ii8yODY1MTI5Ny8zNDE0NDMzNTMtOTQ1MjcwOTQtYjhmYy00ODMyLTkwMmEtMGRmMjdmYTUwNjY2LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDA2MjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwNjIwVDE0MjQzOFomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTFiNTMyMGI3NjNhNjc2MWRkZGUyOWExMGFiYmM3YWQ4NDFhNjhhMzA3MTg5ZWE3YWFiYjBjZTc4YWI0MDFhYzcmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0._RW4w19QuGvlxlwDkz5Was43II6zToLs7Vydpdkr1og)

Various terms and keywords (such as signal names, for example) also have
documentation hints.

### Go To Declaration
Go to declaration for Java symbols and classes;
requires Java support in the IDE and the corresponding project with the classes opened:
![Go to declaration](https://private-user-images.githubusercontent.com/28651297/341443375-5b324717-88e3-409d-8dfa-b0aae69ccda3.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MTg4OTM3NzgsIm5iZiI6MTcxODg5MzQ3OCwicGF0aCI6Ii8yODY1MTI5Ny8zNDE0NDMzNzUtNWIzMjQ3MTctODhlMy00MDlkLThkZmEtYjBhYWU2OWNjZGEzLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDA2MjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwNjIwVDE0MjQzOFomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTBmNjAxMDcxOGQwZThlZTA1MzUwMThjNTU5Yzk3OTRkZGVhMmI0NDczNTUzOTAyZjVkZjJjNmFjZDkxYjBiNjgmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.fuiBKEy1WC2zhCx-Jx3DWN0HbJOTYBdC-XJO9V3uwzI)

### Configuration Options
![Plugin Settings Page](https://private-user-images.githubusercontent.com/28651297/341443338-ada16433-aeb9-4d6d-9b90-7150891b2dae.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MTg4OTI3OTIsIm5iZiI6MTcxODg5MjQ5MiwicGF0aCI6Ii8yODY1MTI5Ny8zNDE0NDMzMzgtYWRhMTY0MzMtYWViOS00ZDZkLTliOTAtNzE1MDg5MWIyZGFlLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDA2MjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwNjIwVDE0MDgxMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTQ0OWVhYmY4YzYxODBiOTA3MmRiMTZlNTQ2YmZlYjNjNTE4N2U2ZjhjYjk5ZDNkYzBhMmZhZTcxZDcyZjQ2NGQmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.PMZ_MZKIBr-Ieyrfce81tGNKYgjlYo4dCK3VIi3MR54)

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "HotSpot Crash Examiner"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/JetBrains/HotSpotCrashExaminerPlugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Resources

* [Java Platform, Standard Edition Troubleshooting Guide. A Fatal Error Log](https://docs.oracle.com/javase/10/troubleshoot/fatal-error-log.htm)
* [YouTube — Volker Simonis — Analyzing HotSpot Crashes](https://www.youtube.com/watch?v=buPX_nj40Tg&t=3078s)
* [YouTube — JVM Crash Dump Analysis](https://www.youtube.com/watch?v=jd6dJa7tSNU)

<!-- Badges -->
[jb-official]: https://github.com/JetBrains#jetbrains-on-github
[jb-official-svg]: https://jb.gg/badges/official.svg

[plugin-repo]: https://github.com/JetBrains/HotSpotCrashExaminerPlugin

[contributions-welcome-svg]: http://img.shields.io/badge/contributions-welcome-brightgreen
[contributions-welcome]: https://github.com/JetBrains/HotSpotCrashExaminerPlugin/blob/master/CONTRIBUTING.md
