cd "C:/Users/Laptop/Documents/kimi/workspace/metla" && git add -A && git commit -m "Внедрены все фичи из анализа APK: WaveformView, PhaseWheel, SignalMeter, SessionLogger, AudioDeviceMonitor, SavedMapPosition, Track/TrackPoint

Новые файлы:
- WaveformView.java — осциллограмма RX-сигнала (400 сэмплов, скользящий буфер)
- PhaseWheel.java — круговой индикатор фазы с сектором железа (-30°...+10°) красным
- SignalMeter.java — стрелочный индикатор амплитуды с дБ-шкалой (-40..0 dB)
- SessionLogger.java — CSV-логирование сессии (time, amplitude_db, phase_deg, i, q, rx_level, lat, lon)
- AudioDeviceMonitor.java — мониторинг подключения проводных/Bluetooth устройств (1 сек)
- SavedMapPosition.java — сохранение/восстановление позиции карты между сессиями
- Track.java — полноценная трек-система с расчётом длины и метаданными
- TrackPoint.java — точка трека с координатами, амплитудой, фазой

MainActivity.java:
- Добавлены поля: sessionLogger, deviceMonitor, currentTrack, waveformView, phaseWheel, signalMeter
- onCreate: инициализация deviceMonitor с callback, запуск мониторинга
- onDestroy: остановка deviceMonitor и sessionLogger
- createLayout: карточки WaveformView, PhaseWheel+SignalMeter row
- startEngine: запуск sessionLogger, создание currentTrack
- stopEngine: остановка sessionLogger, завершение currentTrack
- updateUi: обновление waveformView, phaseWheel, signalMeter; логирование; добавление точки в трек
- recordLoop: waveformView.addSample() на каждом кадре

MapActivity.java:
- Добавлен SavedMapPosition — восстановление позиции при старте, сохранение при onStop
- Убрано жёсткое задание координат Москвы — теперь берётся из savedPosition или GPS" && git push origin main
